package com.srivatchan.apiexplorer.model;

import java.io.Serializable;
import java.util.Base64;

public class BitbucketSession implements Serializable {

    private String workspace;
    private String repoSlug;
    private String email;
    private String appPassword;

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getRepoSlug() {
        return repoSlug;
    }

    public void setRepoSlug(String repoSlug) {
        this.repoSlug = repoSlug;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAppPassword(String appPassword) {
        this.appPassword = appPassword;
    }

    /**
     * Returns the Basic Auth header value: "Basic base64(email:appPassword)"
     */
    public String getBasicAuthHeader() {
        if (email == null || appPassword == null) return null;
        String credentials = email + ":" + appPassword;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    public boolean isConnected() {
        return email != null && !email.isBlank()
                && appPassword != null && !appPassword.isBlank();
    }
}
