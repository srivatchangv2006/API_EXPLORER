package com.srivatchan.apiexplorer.github.service;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.srivatchan.apiexplorer.github.exception.GitHubApiException;
import com.srivatchan.apiexplorer.github.session.GitHubSession;

@Service
public class GitHubApiService {

	private static final String API_BASE = "https://api.github.com";

	private final RestClient restClient = RestClient.create();

	public String getCurrentUser(GitHubSession session) {
		requireAuth(session);
		return get("/user", session.getAccessToken(), false);
	}

	public String listUserRepositories(GitHubSession session) {
		requireAuth(session);
		return get("/user/repos?per_page=100&sort=updated", session.getAccessToken(), false);
	}

	public String getRepositoryInfo(String owner, String repo, GitHubSession session) {
		String token = tokenOrNull(session);
		return get("/repos/" + owner + "/" + repo, token, false);
	}

	public String browsePath(String owner, String repo, String path, GitHubSession session) {
		String token = tokenOrNull(session);
		String apiPath = buildContentsPath(owner, repo, path);
		String json = get(apiPath, token, false);
		String trimmed = json.trim();

		if (trimmed.startsWith("[")) {
			return json;
		}
		if (isFileResponse(trimmed)) {
			return get(apiPath, token, true);
		}
		return json;
	}

	private boolean isFileResponse(String json) {
		return json.contains("\"type\":\"file\"") || json.contains("\"type\": \"file\"");
	}

	public String buildRepoInfoUrl(String owner, String repo) {
		return API_BASE + "/repos/" + owner + "/" + repo;
	}

	public String buildBrowseUrl(String owner, String repo, String path) {
		return API_BASE + buildContentsPath(owner, repo, path);
	}

	private String buildContentsPath(String owner, String repo, String path) {
		if (path == null || path.isBlank()) {
			return "/repos/" + owner + "/" + repo + "/contents";
		}
		return "/repos/" + owner + "/" + repo + "/contents/" + path.trim();
	}

	private String get(String path, String token, boolean raw) {
		String accept = raw ? "application/vnd.github.raw" : "application/vnd.github+json";

		try {
			RestClient.RequestHeadersSpec<?> request = restClient.get()
					.uri(API_BASE + path)
					.header(HttpHeaders.ACCEPT, accept);

			if (token != null && !token.isBlank()) {
				request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
			}

			return request.retrieve().body(String.class);
		}
		catch (RestClientResponseException ex) {
			String body = ex.getResponseBodyAsString();
			if (body == null || body.isBlank()) {
				body = ex.getMessage();
			}
			throw new GitHubApiException(ex.getStatusCode().value(), body);
		}
	}

	private void requireAuth(GitHubSession session) {
		if (session == null || !session.isAuthenticated()) {
			throw new IllegalStateException("This endpoint requires GitHub login.");
		}
	}

	private String tokenOrNull(GitHubSession session) {
		return session != null && session.isAuthenticated() ? session.getAccessToken() : null;
	}
}
