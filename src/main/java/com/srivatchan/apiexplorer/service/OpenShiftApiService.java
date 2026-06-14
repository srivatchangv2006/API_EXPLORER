package com.srivatchan.apiexplorer.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.srivatchan.apiexplorer.model.OpenShiftResource;
import com.srivatchan.apiexplorer.model.OpenShiftSession;

@Service
public class OpenShiftApiService {

	private final RestClient restClient = RestClient.create();

	public String fetchResource(OpenShiftSession session, OpenShiftResource resource) {
		if (!session.isConnected()) {
			throw new IllegalStateException("Not connected to OpenShift. Paste your login command first.");
		}

		String path = resource.buildPath(session.getNamespace());
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
