package com.srivatchan.apiexplorer.github.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srivatchan.apiexplorer.github.service.GitHubAuthService;

@Controller
@RequestMapping("/github")
public class GitHubAuthController {

	private final GitHubAuthService gitHubAuthService;

	public GitHubAuthController(GitHubAuthService gitHubAuthService) {
		this.gitHubAuthService = gitHubAuthService;
	}

	@GetMapping("/login")
	public String login(RedirectAttributes redirectAttributes) {
		try {
			return "redirect:" + gitHubAuthService.getAuthorizationUrl();
		}
		catch (IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/";
		}
	}

	@GetMapping("/callback")
	public String callback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String error,
			@RequestParam(name = "error_description", required = false) String errorDescription,
			HttpSession httpSession,
			RedirectAttributes redirectAttributes) {
		if (error != null) {
			String message = errorDescription != null ? errorDescription : error;
			redirectAttributes.addFlashAttribute("error", "GitHub authorization failed: " + message);
			return "redirect:/";
		}
		if (code == null || code.isBlank()) {
			redirectAttributes.addFlashAttribute("error", "GitHub did not return an authorization code.");
			return "redirect:/";
		}

		try {
			gitHubAuthService.authenticate(code, httpSession);
			return "redirect:/github/explorer";
		}
		catch (Exception ex) {
			redirectAttributes.addFlashAttribute("error", "GitHub login failed: " + ex.getMessage());
			return "redirect:/";
		}
	}

	@PostMapping("/logout")
	public String logout(HttpSession httpSession) {
		gitHubAuthService.logout(httpSession);
		return "redirect:/github/explorer";
	}
}
