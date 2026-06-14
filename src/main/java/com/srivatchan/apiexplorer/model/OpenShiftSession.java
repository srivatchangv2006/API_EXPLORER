package com.srivatchan.apiexplorer.model;

import java.io.Serializable;

public class OpenShiftSession implements Serializable {

	private String cluster;
	private String namespace;
	private String server;
	private String token;

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
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
