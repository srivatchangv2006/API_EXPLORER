package com.srivatchan.openshiftui.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NamespaceOverview {

	private String name;
	private int deploymentCount;
	private int podCount;
	private Map<String, Integer> podsByPhase = new LinkedHashMap<>();
	private int serviceCount;
	private int routeCount;
	private List<String> routeHosts = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDeploymentCount() {
		return deploymentCount;
	}

	public void setDeploymentCount(int deploymentCount) {
		this.deploymentCount = deploymentCount;
	}

	public int getPodCount() {
		return podCount;
	}

	public void setPodCount(int podCount) {
		this.podCount = podCount;
	}

	public Map<String, Integer> getPodsByPhase() {
		return podsByPhase;
	}

	public void setPodsByPhase(Map<String, Integer> podsByPhase) {
		this.podsByPhase = podsByPhase;
	}

	public int getServiceCount() {
		return serviceCount;
	}

	public void setServiceCount(int serviceCount) {
		this.serviceCount = serviceCount;
	}

	public int getRouteCount() {
		return routeCount;
	}

	public void setRouteCount(int routeCount) {
		this.routeCount = routeCount;
	}

	public List<String> getRouteHosts() {
		return routeHosts;
	}

	public void setRouteHosts(List<String> routeHosts) {
		this.routeHosts = routeHosts;
	}
}
