package com.srivatchan.openshiftui.session;

import java.io.Serializable;

public class OpenShiftUiSession implements Serializable {

	private String cluster;
	private String server;
	private String token;

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean isConnected() {
		return server != null && !server.isBlank()
				&& token != null && !token.isBlank();
	}
}
