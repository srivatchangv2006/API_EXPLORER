package com.srivatchan.apiexplorer.model;

public enum OpenShiftResource {

	PROJECTS("Projects", "/apis/project.openshift.io/v1/projects", false),
	PODS("Pods", "/api/v1/namespaces/{namespace}/pods", true),
	DEPLOYMENTS("Deployments", "/apis/apps/v1/namespaces/{namespace}/deployments", true),
	SERVICES("Services", "/api/v1/namespaces/{namespace}/services", true),
	ROUTES("Routes", "/apis/route.openshift.io/v1/namespaces/{namespace}/routes", true),
	CONFIGMAPS("ConfigMaps", "/api/v1/namespaces/{namespace}/configmaps", true),
	SECRETS("Secrets", "/api/v1/namespaces/{namespace}/secrets", true),
	REPLICASETS("ReplicaSets", "/apis/apps/v1/namespaces/{namespace}/replicasets", true),
	STATEFULSETS("StatefulSets", "/apis/apps/v1/namespaces/{namespace}/statefulsets", true),
	JOBS("Jobs", "/apis/batch/v1/namespaces/{namespace}/jobs", true),
	CRONJOBS("CronJobs", "/apis/batch/v1/namespaces/{namespace}/cronjobs", true);

	private final String label;
	private final String pathTemplate;
	private final boolean namespaceScoped;

	OpenShiftResource(String label, String pathTemplate, boolean namespaceScoped) {
		this.label = label;
		this.pathTemplate = pathTemplate;
		this.namespaceScoped = namespaceScoped;
	}

	public String getLabel() {
		return label;
	}

	public String getPathTemplate() {
		return pathTemplate;
	}

	public boolean isNamespaceScoped() {
		return namespaceScoped;
	}

	public String buildPath(String namespace) {
		if (namespaceScoped) {
			return pathTemplate.replace("{namespace}", namespace);
		}
		return pathTemplate;
	}
}
