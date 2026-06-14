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

import com.srivatchan.apiexplorer.config.OpenShiftClusterProperties;
import com.srivatchan.apiexplorer.model.OpenShiftResource;
import com.srivatchan.apiexplorer.model.OpenShiftSession;
import com.srivatchan.apiexplorer.service.LoginCommandParser;
import com.srivatchan.apiexplorer.service.LoginCommandParser.ParsedLoginCommand;
import com.srivatchan.apiexplorer.service.OpenShiftApiException;
import com.srivatchan.apiexplorer.service.OpenShiftApiService;

@Controller
@RequestMapping("/openshift")
public class OpenShiftController {

	public static final String SESSION_KEY = "openshiftSession";

	private final OpenShiftClusterProperties clusterProperties;
	private final LoginCommandParser loginCommandParser;
	private final OpenShiftApiService openShiftApiService;

	public OpenShiftController(
			OpenShiftClusterProperties clusterProperties,
			LoginCommandParser loginCommandParser,
			OpenShiftApiService openShiftApiService) {
		this.clusterProperties = clusterProperties;
		this.loginCommandParser = loginCommandParser;
		this.openShiftApiService = openShiftApiService;
	}

	@PostMapping("/connect")
	public String connect(
			@RequestParam String cluster,
			@RequestParam String namespace,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		var clusterInfo = clusterProperties.findCluster(cluster);
		if (clusterInfo.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Unknown cluster: " + cluster);
			return "redirect:/";
		}

		OpenShiftSession openshiftSession = getOrCreateSession(session);
		openshiftSession.setCluster(cluster.trim());
		openshiftSession.setNamespace(namespace.trim());
		openshiftSession.setServer(null);
		openshiftSession.setToken(null);
		session.setAttribute(SESSION_KEY, openshiftSession);

		return "redirect:/openshift/login";
	}

	@GetMapping("/login")
	public String loginPage(HttpSession session, Model model) {
		OpenShiftSession openshiftSession = getSession(session);
		if (openshiftSession == null || openshiftSession.getCluster() == null) {
			return "redirect:/";
		}

		var clusterInfo = clusterProperties.findCluster(openshiftSession.getCluster());
		if (clusterInfo.isEmpty()) {
			return "redirect:/";
		}

		model.addAttribute("cluster", openshiftSession.getCluster());
		model.addAttribute("namespace", openshiftSession.getNamespace());
		model.addAttribute("consoleUrl", clusterInfo.get().getConsoleUrl());
		return "openshift-login";
	}

	@GetMapping("/paste")
	public String pastePage(HttpSession session, Model model) {
		OpenShiftSession openshiftSession = getSession(session);
		if (openshiftSession == null || openshiftSession.getCluster() == null) {
			return "redirect:/";
		}

		model.addAttribute("cluster", openshiftSession.getCluster());
		model.addAttribute("namespace", openshiftSession.getNamespace());
		return "openshift-paste";
	}

	@PostMapping("/paste")
	public String pasteCommand(
			@RequestParam String loginCommand,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		OpenShiftSession openshiftSession = getSession(session);
		if (openshiftSession == null) {
			return "redirect:/";
		}

		try {
			ParsedLoginCommand parsed = loginCommandParser.parse(loginCommand);
			openshiftSession.setToken(parsed.token());
			openshiftSession.setServer(parsed.server());
			session.setAttribute(SESSION_KEY, openshiftSession);
			return "redirect:/openshift/explorer";
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/openshift/paste";
		}
	}

	@GetMapping("/explorer")
	public String explorer(HttpSession session, Model model) {
		OpenShiftSession openshiftSession = getSession(session);
		if (openshiftSession == null || !openshiftSession.isConnected()) {
			return "redirect:/openshift/paste";
		}

		model.addAttribute("openshift", openshiftSession);
		model.addAttribute("resources", OpenShiftResource.values());
		return "openshift-explorer";
	}

	@PostMapping("/api/{resource}")
	public String callApi(
			@PathVariable String resource,
			HttpSession session,
			Model model) {
		OpenShiftSession openshiftSession = getSession(session);
		if (openshiftSession == null || !openshiftSession.isConnected()) {
			return "redirect:/openshift/paste";
		}

		OpenShiftResource openshiftResource;
		try {
			openshiftResource = OpenShiftResource.valueOf(resource.toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			populateExplorerModel(model, openshiftSession);
			model.addAttribute("error", "Unknown resource: " + resource);
			model.addAttribute("responseBody", "");
			return "openshift-explorer";
		}

		populateExplorerModel(model, openshiftSession);
		model.addAttribute("resources", OpenShiftResource.values());
		model.addAttribute("activeResource", openshiftResource);

		try {
			String responseBody = openShiftApiService.fetchResource(openshiftSession, openshiftResource);
			model.addAttribute("responseBody", responseBody);
		}
		catch (OpenShiftApiException ex) {
			model.addAttribute("error", "OpenShift API error (" + ex.getStatusCode() + ")");
			model.addAttribute("responseBody", ex.getMessage());
		}
		catch (Exception ex) {
			model.addAttribute("error", "Request failed: " + ex.getMessage());
			model.addAttribute("responseBody", "");
		}

		return "openshift-explorer";
	}

	@PostMapping("/disconnect")
	public String disconnect(HttpSession session) {
		session.removeAttribute(SESSION_KEY);
		return "redirect:/";
	}

	private OpenShiftSession getSession(HttpSession session) {
		return (OpenShiftSession) session.getAttribute(SESSION_KEY);
	}

	private OpenShiftSession getOrCreateSession(HttpSession session) {
		OpenShiftSession openshiftSession = getSession(session);
		if (openshiftSession == null) {
			openshiftSession = new OpenShiftSession();
		}
		return openshiftSession;
	}

	private void populateExplorerModel(Model model, OpenShiftSession openshiftSession) {
		model.addAttribute("openshift", openshiftSession);
		model.addAttribute("resources", OpenShiftResource.values());
	}
}
