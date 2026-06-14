package com.srivatchan.apiexplorer.github.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.srivatchan.apiexplorer.github.exception.GitHubApiException;
import com.srivatchan.apiexplorer.github.service.GitHubApiService;
import com.srivatchan.apiexplorer.github.service.GitHubAuthService;
import com.srivatchan.apiexplorer.github.session.GitHubSession;

@Controller
@RequestMapping("/github")
public class GitHubExplorerController {

	private final GitHubAuthService gitHubAuthService;
	private final GitHubApiService gitHubApiService;

	public GitHubExplorerController(GitHubAuthService gitHubAuthService, GitHubApiService gitHubApiService) {
		this.gitHubAuthService = gitHubAuthService;
		this.gitHubApiService = gitHubApiService;
	}

	@GetMapping("/explorer")
	public String explorer(HttpSession httpSession, Model model) {
		populateExplorerModel(model, gitHubAuthService.getSession(httpSession));
		return "github-explorer";
	}

	@PostMapping("/api/user-repos")
	public String listUserRepos(HttpSession httpSession, Model model) {
		return execute(httpSession, model, "user-repos", () ->
				gitHubApiService.listUserRepositories(requireSession(httpSession)));
	}

	@PostMapping("/api/current-user")
	public String currentUser(HttpSession httpSession, Model model) {
		return execute(httpSession, model, "current-user", () ->
				gitHubApiService.getCurrentUser(requireSession(httpSession)));
	}

	@PostMapping("/api/repo-info")
	public String repoInfo(
			@RequestParam String owner,
			@RequestParam String repo,
			HttpSession httpSession,
			Model model) {
		GitHubSession session = gitHubAuthService.getSession(httpSession);
		saveRepoSelection(session, httpSession, owner, repo);
		populateExplorerModel(model, session);
		model.addAttribute("activeAction", "repo-info");
		model.addAttribute("lastUrl", gitHubApiService.buildRepoInfoUrl(owner.trim(), repo.trim()));

		return executeResponse(model, () ->
				gitHubApiService.getRepositoryInfo(owner.trim(), repo.trim(), session));
	}

	@PostMapping("/api/browse")
	public String browsePath(
			@RequestParam String owner,
			@RequestParam String repo,
			@RequestParam(required = false, defaultValue = "") String path,
			HttpSession httpSession,
			Model model) {
		GitHubSession session = gitHubAuthService.getSession(httpSession);
		saveRepoSelection(session, httpSession, owner, repo);
		populateExplorerModel(model, session);
		model.addAttribute("path", path);
		model.addAttribute("activeAction", "browse");
		model.addAttribute("lastUrl", gitHubApiService.buildBrowseUrl(owner.trim(), repo.trim(), path));

		return executeResponse(model, () ->
				gitHubApiService.browsePath(owner.trim(), repo.trim(), path, session));
	}

	private String execute(HttpSession httpSession, Model model, String action, ApiCall call) {
		populateExplorerModel(model, gitHubAuthService.getSession(httpSession));
		model.addAttribute("activeAction", action);
		return executeResponse(model, call);
	}

	private String executeResponse(Model model, ApiCall call) {
		try {
			model.addAttribute("responseBody", call.run());
		}
		catch (GitHubApiException ex) {
			model.addAttribute("error", "GitHub API error (" + ex.getStatusCode() + ")");
			model.addAttribute("responseBody", ex.getMessage());
		}
		catch (Exception ex) {
			model.addAttribute("error", ex.getMessage());
			model.addAttribute("responseBody", "");
		}
		return "github-explorer";
	}

	private GitHubSession requireSession(HttpSession httpSession) {
		GitHubSession session = gitHubAuthService.getSession(httpSession);
		if (session == null || !session.isAuthenticated()) {
			throw new IllegalStateException("Please log in with GitHub first.");
		}
		return session;
	}

	private void saveRepoSelection(GitHubSession session, HttpSession httpSession, String owner, String repo) {
		if (session != null) {
			session.setSelectedOwner(owner.trim());
			session.setSelectedRepo(repo.trim());
			httpSession.setAttribute(GitHubAuthService.SESSION_KEY, session);
		}
	}

	private void populateExplorerModel(Model model, GitHubSession session) {
		model.addAttribute("github", session);
		model.addAttribute("connected", session != null && session.isAuthenticated());
		if (session != null) {
			model.addAttribute("owner", session.getSelectedOwner() != null ? session.getSelectedOwner() : "");
			model.addAttribute("repo", session.getSelectedRepo() != null ? session.getSelectedRepo() : "");
		}
		else {
			model.addAttribute("owner", "");
			model.addAttribute("repo", "");
		}
		if (!model.containsAttribute("path")) {
			model.addAttribute("path", "");
		}
	}

	@FunctionalInterface
	private interface ApiCall {
		String run();
	}
}
