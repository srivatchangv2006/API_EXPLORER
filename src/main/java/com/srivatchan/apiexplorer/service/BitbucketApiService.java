package com.srivatchan.apiexplorer.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.srivatchan.apiexplorer.model.BitbucketResource;
import com.srivatchan.apiexplorer.model.BitbucketSession;

@Service
public class BitbucketApiService {

    private final RestClient restClient = RestClient.create();

    public String fetchResource(BitbucketSession session,
                                BitbucketResource resource,
                                String filePath) {
        if (!session.isConnected()) {
            throw new IllegalStateException("Not connected to Bitbucket. Enter your credentials first.");
        }

        String url = resource.buildUrl(session.getWorkspace(), session.getRepoSlug(), filePath);

        try {
            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, session.getBasicAuthHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (body == null || body.isBlank()) {
                body = ex.getMessage();
            }
            throw new BitbucketApiException(ex.getStatusCode().value(), body);
        }
    }
}
