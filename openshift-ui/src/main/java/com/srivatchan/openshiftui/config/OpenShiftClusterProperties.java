package com.srivatchan.openshiftui.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openshift")
public class OpenShiftClusterProperties {

	private Map<String, ClusterInfo> clusters = new HashMap<>();

	public Map<String, ClusterInfo> getClusters() {
		return clusters;
	}

	public void setClusters(Map<String, ClusterInfo> clusters) {
		this.clusters = clusters;
	}

	public Optional<ClusterInfo> findCluster(String name) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(clusters.get(name.trim().toLowerCase()));
	}

	public static class ClusterInfo {

		private String consoleUrl;

		public String getConsoleUrl() {
			return consoleUrl;
		}

		public void setConsoleUrl(String consoleUrl) {
			this.consoleUrl = consoleUrl;
		}
	}
}
