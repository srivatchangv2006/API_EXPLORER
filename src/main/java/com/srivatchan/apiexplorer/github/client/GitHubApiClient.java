package com.srivatchan.apiexplorer.github.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.srivatchan.apiexplorer.github.config.GitHubOAuthProperties;
import com.srivatchan.apiexplorer.github.dto.GitHubTokenResponse;
import com.srivatchan.apiexplorer.github.dto.GitHubUserResponse;
import com.srivatchan.apiexplorer.github.exception.GitHubApiException;

@Component
public class GitHubApiClient {

	private static final String API_BASE = "https://api.github.com";
	private static final String OAUTH_TOKEN_URL = "https://github.com/login/oauth/access_token";

	private final RestClient apiClient = RestClient.create();
	private final RestClient oauthClient = RestClient.create();
	private final GitHubOAuthProperties oauthProperties;

	public GitHubApiClient(GitHubOAuthProperties oauthProperties) {
		this.oauthProperties = oauthProperties;
	}

	public String buildAuthorizationUrl() {
		return "https://github.com/login/oauth/authorize"
				+ "?client_id=" + encode(oauthProperties.getClientId())
				+ "&redirect_uri=" + encode(oauthProperties.getRedirectUri())
				+ "&scope=" + encode(oauthProperties.getScope());
	}

	public GitHubTokenResponse exchangeCodeForToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", oauthProperties.getClientId());
		form.add("client_secret", oauthProperties.getClientSecret());
		form.add("code", code);

		GitHubTokenResponse response = oauthClient.post()
				.uri(OAUTH_TOKEN_URL)
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(GitHubTokenResponse.class);

		if (response == null) {
			throw new GitHubApiException(500, "Empty response from GitHub token endpoint.");
		}
		if (response.getError() != null) {
			String message = response.getErrorDescription() != null
					? response.getErrorDescription()
					: response.getError();
			throw new GitHubApiException(400, message);
		}
		if (response.getAccessToken() == null || response.getAccessToken().isBlank()) {
			throw new GitHubApiException(400, "GitHub did not return an access token.");
		}
		return response;
	}

	public GitHubUserResponse getCurrentUser(String token) {
		return apiClient.get()
				.uri(API_BASE + "/user")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
				.retrieve()
				.body(GitHubUserResponse.class);
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
