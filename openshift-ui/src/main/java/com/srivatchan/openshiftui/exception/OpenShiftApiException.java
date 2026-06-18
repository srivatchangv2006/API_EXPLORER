package com.srivatchan.openshiftui.exception;

public class OpenShiftApiException extends RuntimeException {

	private final int statusCode;

	public OpenShiftApiException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
