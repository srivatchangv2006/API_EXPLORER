package com.srivatchan.openshiftui.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.srivatchan.openshiftui.exception.OpenShiftApiException;
import com.srivatchan.openshiftui.session.OpenShiftUiSession;

@Component
public class OpenShiftApiClient {

	private final RestClient restClient = RestClient.create();

	public String get(OpenShiftUiSession session, String path) {
		if (!session.isConnected()) {
			throw new IllegalStateException("Not connected to OpenShift.");
		}

		String url = session.getServer().replaceAll("/$", "") + path;

		try {
			return restClient.get()
					.uri(url)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getToken())
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.body(String.class);
		}
		catch (RestClientResponseException ex) {
			String body = ex.getResponseBodyAsString();
			if (body == null || body.isBlank()) {
				body = ex.getMessage();
			}
			throw new OpenShiftApiException(ex.getStatusCode().value(), body);
		}
	}
}
