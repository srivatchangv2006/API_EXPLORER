package com.srivatchan.apiexplorer.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubOAuthProperties {

	private String clientId;
	private String clientSecret;
	private String redirectUri;
	private String scope = "repo read:user";

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank()
				&& clientSecret != null && !clientSecret.isBlank()
				&& redirectUri != null && !redirectUri.isBlank();
	}
}
