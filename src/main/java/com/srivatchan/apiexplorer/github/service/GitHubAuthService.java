package com.srivatchan.apiexplorer.github.service;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import com.srivatchan.apiexplorer.github.client.GitHubApiClient;
import com.srivatchan.apiexplorer.github.config.GitHubOAuthProperties;
import com.srivatchan.apiexplorer.github.dto.GitHubTokenResponse;
import com.srivatchan.apiexplorer.github.dto.GitHubUserResponse;
import com.srivatchan.apiexplorer.github.session.GitHubSession;

@Service
public class GitHubAuthService {

	public static final String SESSION_KEY = "githubSession";

	private final GitHubOAuthProperties oauthProperties;
	private final GitHubApiClient gitHubApiClient;

	public GitHubAuthService(GitHubOAuthProperties oauthProperties, GitHubApiClient gitHubApiClient) {
		this.oauthProperties = oauthProperties;
		this.gitHubApiClient = gitHubApiClient;
	}

	public void ensureConfigured() {
		if (!oauthProperties.isConfigured()) {
			throw new IllegalStateException(
					"GitHub OAuth is not configured. Set github.oauth.client-id and github.oauth.client-secret.");
		}
	}

	public String getAuthorizationUrl() {
		ensureConfigured();
		return gitHubApiClient.buildAuthorizationUrl();
	}

	public GitHubSession authenticate(String code, HttpSession httpSession) {
		ensureConfigured();
		GitHubTokenResponse tokenResponse = gitHubApiClient.exchangeCodeForToken(code);
		GitHubUserResponse user = gitHubApiClient.getCurrentUser(tokenResponse.getAccessToken());

		GitHubSession session = new GitHubSession();
		session.setAccessToken(tokenResponse.getAccessToken());
		session.setGithubUsername(user.getLogin());
		session.setGithubUserId(user.getId());
		httpSession.setAttribute(SESSION_KEY, session);
		return session;
	}

	public GitHubSession getSession(HttpSession httpSession) {
		return (GitHubSession) httpSession.getAttribute(SESSION_KEY);
	}

	public void logout(HttpSession httpSession) {
		httpSession.removeAttribute(SESSION_KEY);
		httpSession.invalidate();
	}
}
