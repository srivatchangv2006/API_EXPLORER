package com.srivatchan.apiexplorer.github.session;

import java.io.Serializable;

public class GitHubSession implements Serializable {

	private String accessToken;
	private String githubUsername;
	private Long githubUserId;
	private String selectedOwner;
	private String selectedRepo;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getGithubUsername() {
		return githubUsername;
	}

	public void setGithubUsername(String githubUsername) {
		this.githubUsername = githubUsername;
	}

	public Long getGithubUserId() {
		return githubUserId;
	}

	public void setGithubUserId(Long githubUserId) {
		this.githubUserId = githubUserId;
	}

	public String getSelectedOwner() {
		return selectedOwner;
	}

	public void setSelectedOwner(String selectedOwner) {
		this.selectedOwner = selectedOwner;
	}

	public String getSelectedRepo() {
		return selectedRepo;
	}

	public void setSelectedRepo(String selectedRepo) {
		this.selectedRepo = selectedRepo;
	}

	public boolean isAuthenticated() {
		return accessToken != null && !accessToken.isBlank();
	}
}
