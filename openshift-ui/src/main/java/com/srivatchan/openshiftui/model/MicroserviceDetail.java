package com.srivatchan.openshiftui.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MicroserviceDetail {

	private String name;
	private String namespace;
	private String podsSummary;
	private List<PodInfo> pods = new ArrayList<>();
	private Map<String, Map<String, String>> configMaps = new LinkedHashMap<>();
	private Map<String, Map<String, String>> secrets = new LinkedHashMap<>();
	private String applicationVersion;
	private List<CertificateInfo> certificates = new ArrayList<>();
	private List<String> routes = new ArrayList<>();
	private List<ServiceInfo> services = new ArrayList<>();
	private List<String> externalDependencies = new ArrayList<>();
	private List<DetailRow> detailRows = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getPodsSummary() {
		return podsSummary;
	}

	public void setPodsSummary(String podsSummary) {
		this.podsSummary = podsSummary;
	}

	public List<PodInfo> getPods() {
		return pods;
	}

	public void setPods(List<PodInfo> pods) {
		this.pods = pods;
	}

	public Map<String, Map<String, String>> getConfigMaps() {
		return configMaps;
	}

	public void setConfigMaps(Map<String, Map<String, String>> configMaps) {
		this.configMaps = configMaps;
	}

	public Map<String, Map<String, String>> getSecrets() {
		return secrets;
	}

	public void setSecrets(Map<String, Map<String, String>> secrets) {
		this.secrets = secrets;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public List<CertificateInfo> getCertificates() {
		return certificates;
	}

	public void setCertificates(List<CertificateInfo> certificates) {
		this.certificates = certificates;
	}

	public List<String> getRoutes() {
		return routes;
	}

	public void setRoutes(List<String> routes) {
		this.routes = routes;
	}

	public List<ServiceInfo> getServices() {
		return services;
	}

	public void setServices(List<ServiceInfo> services) {
		this.services = services;
	}

	public List<String> getExternalDependencies() {
		return externalDependencies;
	}

	public void setExternalDependencies(List<String> externalDependencies) {
		this.externalDependencies = externalDependencies;
	}

	public List<DetailRow> getDetailRows() {
		return detailRows;
	}

	public void setDetailRows(List<DetailRow> detailRows) {
		this.detailRows = detailRows;
	}

	public record PodInfo(String name, String phase) {
	}

	public record CertificateInfo(String name, List<String> keys) {
	}

	public record ServiceInfo(String name, String ports, String selector) {
	}

	public record DetailRow(String key, String value) {
	}
}
