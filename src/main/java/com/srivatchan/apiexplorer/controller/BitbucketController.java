package com.srivatchan.apiexplorer.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srivatchan.apiexplorer.model.BitbucketResource;
import com.srivatchan.apiexplorer.model.BitbucketSession;
import com.srivatchan.apiexplorer.service.BitbucketApiException;
import com.srivatchan.apiexplorer.service.BitbucketApiService;

@Controller
@RequestMapping("/bitbucket")
public class BitbucketController {

    public static final String SESSION_KEY = "bitbucketSession";

    private final BitbucketApiService bitbucketApiService;

    public BitbucketController(BitbucketApiService bitbucketApiService) {
        this.bitbucketApiService = bitbucketApiService;
    }

    // ── Step 1: collect workspace + repoSlug ─────────────────────────────────

    @PostMapping("/connect")
    public String connect(
            @RequestParam String workspace,
            @RequestParam String repoSlug,
            HttpSession session) {

        BitbucketSession bbSession = getOrCreate(session);
        bbSession.setWorkspace(workspace.trim());
        bbSession.setRepoSlug(repoSlug.trim());
        bbSession.setEmail(null);
        bbSession.setAppPassword(null);
        session.setAttribute(SESSION_KEY, bbSession);

        return "redirect:/bitbucket/login";
    }

    // ── Step 2: instructions page ─────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        BitbucketSession bbSession = getSession(session);
        if (bbSession == null || bbSession.getWorkspace() == null) {
            return "redirect:/";
        }
        model.addAttribute("bitbucket", bbSession);
        return "bitbucket-login";
    }

    // ── Step 3: credentials entry ─────────────────────────────────────────────

    @GetMapping("/credentials")
    public String credentialsPage(HttpSession session, Model model) {
        BitbucketSession bbSession = getSession(session);
        if (bbSession == null || bbSession.getWorkspace() == null) {
            return "redirect:/";
        }
        model.addAttribute("bitbucket", bbSession);
        return "bitbucket-credentials";
    }

    @PostMapping("/credentials")
    public String submitCredentials(
            @RequestParam String email,
            @RequestParam String appPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        BitbucketSession bbSession = getSession(session);
        if (bbSession == null) {
            return "redirect:/";
        }

        if (email == null || email.isBlank() || appPassword == null || appPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email and App Password are required.");
            return "redirect:/bitbucket/credentials";
        }

        bbSession.setEmail(email.trim());
        bbSession.setAppPassword(appPassword.trim());
        session.setAttribute(SESSION_KEY, bbSession);

        return "redirect:/bitbucket/explorer";
    }

    // ── Step 4: explorer ──────────────────────────────────────────────────────

    @GetMapping("/explorer")
    public String explorer(HttpSession session, Model model) {
        BitbucketSession bbSession = getSession(session);
        if (bbSession == null || !bbSession.isConnected()) {
            return "redirect:/bitbucket/credentials";
        }
        model.addAttribute("bitbucket", bbSession);
        model.addAttribute("resources", BitbucketResource.values());
        return "bitbucket-explorer";
    }

    @PostMapping("/api/{resource}")
    public String callApi(
            @PathVariable String resource,
            @RequestParam(required = false) String filePath,
            HttpSession session,
            Model model) {

        BitbucketSession bbSession = getSession(session);
        if (bbSession == null || !bbSession.isConnected()) {
            return "redirect:/bitbucket/credentials";
        }

        BitbucketResource bbResource;
        try {
            bbResource = BitbucketResource.valueOf(resource.toUpperCase());
        } catch (IllegalArgumentException ex) {
            populateExplorerModel(model, bbSession);
            model.addAttribute("error", "Unknown resource: " + resource);
            model.addAttribute("responseBody", "");
            return "bitbucket-explorer";
        }

        populateExplorerModel(model, bbSession);
        model.addAttribute("activeResource", bbResource);

        // BROWSE_FILE requires a filePath — bounce back with error if missing
        if (bbResource.requiresFilePath() && (filePath == null || filePath.isBlank())) {
            model.addAttribute("error", "Please enter a file or directory path (e.g. main/README.md).");
            model.addAttribute("responseBody", "");
            model.addAttribute("showFileInput", true);
            return "bitbucket-explorer";
        }

        try {
            String responseBody = bitbucketApiService.fetchResource(bbSession, bbResource, filePath);
            model.addAttribute("responseBody", responseBody);
            if (bbResource.requiresFilePath()) {
                model.addAttribute("showFileInput", true);
                model.addAttribute("filePath", filePath);
            }
        } catch (BitbucketApiException ex) {
            model.addAttribute("error", "Bitbucket API error (" + ex.getStatusCode() + ")");
            model.addAttribute("responseBody", ex.getMessage());
        } catch (Exception ex) {
            model.addAttribute("error", "Request failed: " + ex.getMessage());
            model.addAttribute("responseBody", "");
        }

        return "bitbucket-explorer";
    }

    // ── Disconnect ────────────────────────────────────────────────────────────

    @PostMapping("/disconnect")
    public String disconnect(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return "redirect:/";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BitbucketSession getSession(HttpSession session) {
        return (BitbucketSession) session.getAttribute(SESSION_KEY);
    }

    private BitbucketSession getOrCreate(HttpSession session) {
        BitbucketSession s = getSession(session);
        return s != null ? s : new BitbucketSession();
    }

    private void populateExplorerModel(Model model, BitbucketSession bbSession) {
        model.addAttribute("bitbucket", bbSession);
        model.addAttribute("resources", BitbucketResource.values());
    }
}
