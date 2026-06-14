package com.srivatchan.apiexplorer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LoginCommandParserTest {

	private final LoginCommandParser parser = new LoginCommandParser();

	@Test
	void parsesSingleLineCommand() {
		String command = "oc login --token=sha256~abc123 --server=https://api.rm3.7wse.p1.openshiftapps.com:6443";
		var parsed = parser.parse(command);
		assertEquals("sha256~abc123", parsed.token());
		assertEquals("https://api.rm3.7wse.p1.openshiftapps.com:6443", parsed.server());
	}

	@Test
	void parsesMultilineCommand() {
		String command = """
				oc login --token=sha256~abc123 \\
				--server=https://api.rm3.7wse.p1.openshiftapps.com:6443
				""";
		var parsed = parser.parse(command);
		assertEquals("sha256~abc123", parsed.token());
		assertEquals("https://api.rm3.7wse.p1.openshiftapps.com:6443", parsed.server());
	}

	@Test
	void rejectsMissingToken() {
		assertThrows(IllegalArgumentException.class,
				() -> parser.parse("oc login --server=https://example.com:6443"));
	}

	@Test
	void parsesTokenWithUnderscoresAndHyphens() {
		String command = """
				oc login --token=sha256~Ql6g4ZeEARDkIgAXwHd10E9CNou0Nf2a0L_4kT9b-bo \\
				--server=https://api.rm3.7wse.p1.openshiftapps.com:6443
				""";
		var parsed = parser.parse(command);
		assertEquals("sha256~Ql6g4ZeEARDkIgAXwHd10E9CNou0Nf2a0L_4kT9b-bo", parsed.token());
		assertEquals("https://api.rm3.7wse.p1.openshiftapps.com:6443", parsed.server());
	}
}
