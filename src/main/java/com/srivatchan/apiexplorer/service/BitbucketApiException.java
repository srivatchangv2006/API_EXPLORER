package com.srivatchan.apiexplorer.service;

public class BitbucketApiException extends RuntimeException {

    private final int statusCode;

    public BitbucketApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
