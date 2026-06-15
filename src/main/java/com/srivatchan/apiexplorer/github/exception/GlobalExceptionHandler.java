package com.srivatchan.apiexplorer.github.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "com.srivatchan.apiexplorer.github")
public class GlobalExceptionHandler {

	@ExceptionHandler(GitHubApiException.class)
	public String handleGitHubApiException(GitHubApiException ex, Model model) {
		model.addAttribute("error", "GitHub API error (" + ex.getStatusCode() + "): " + ex.getMessage());
		return "github-error";
	}
}
