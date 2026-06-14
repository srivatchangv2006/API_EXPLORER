package com.srivatchan.apiexplorer.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class LoginCommandParser {

	private static final Pattern TOKEN_PATTERN = Pattern.compile("--token=(\\S+)");
	private static final Pattern SERVER_PATTERN = Pattern.compile("--server=(\\S+)");

	public ParsedLoginCommand parse(String loginCommand) {
		if (loginCommand == null || loginCommand.isBlank()) {
			throw new IllegalArgumentException("Login command cannot be empty.");
		}

		String normalized = loginCommand.replace("\\\n", " ").replace('\n', ' ').trim();

		Matcher tokenMatcher = TOKEN_PATTERN.matcher(normalized);
		if (!tokenMatcher.find()) {
			throw new IllegalArgumentException("Could not find --token= in the login command.");
		}

		Matcher serverMatcher = SERVER_PATTERN.matcher(normalized);
		if (!serverMatcher.find()) {
			throw new IllegalArgumentException("Could not find --server= in the login command.");
		}

		String token = tokenMatcher.group(1).trim();
		String server = serverMatcher.group(1).trim();

		if (token.isBlank() || server.isBlank()) {
			throw new IllegalArgumentException("Token and server must not be empty.");
		}

		return new ParsedLoginCommand(token, server);
	}

	public record ParsedLoginCommand(String token, String server) {
	}
}
