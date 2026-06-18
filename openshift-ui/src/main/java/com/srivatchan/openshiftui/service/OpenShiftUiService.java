package com.srivatchan.openshiftui.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.srivatchan.openshiftui.client.OpenShiftApiClient;
import com.srivatchan.openshiftui.model.MicroserviceDetail;
import com.srivatchan.openshiftui.model.NamespaceOverview;
import com.srivatchan.openshiftui.model.MicroserviceDetail.CertificateInfo;
import com.srivatchan.openshiftui.model.MicroserviceDetail.DetailRow;
import com.srivatchan.openshiftui.model.MicroserviceDetail.PodInfo;
import com.srivatchan.openshiftui.model.MicroserviceDetail.ServiceInfo;
import com.srivatchan.openshiftui.session.OpenShiftUiSession;

@Service
public class OpenShiftUiService {

	private static final Set<String> DEPENDENCY_KEYWORDS = Set.of(
			"mysql", "postgres", "postgresql", "kafka", "redis", "mongo", "mongodb",
			"rabbitmq", "mariadb", "oracle", "elastic", "cassandra", "jdbc:");

	private final OpenShiftApiClient apiClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public OpenShiftUiService(OpenShiftApiClient apiClient) {
		this.apiClient = apiClient;
	}

	public List<String> listNamespaces(OpenShiftUiSession session) {
		JsonNode root = parse(apiClient.get(session, "/apis/project.openshift.io/v1/projects"));
		List<String> names = new ArrayList<>();
		for (JsonNode item : iterable(root.path("items"))) {
			names.add(item.path("metadata").path("name").asText());
		}
		names.removeIf(String::isBlank);
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	public NamespaceOverview getNamespaceOverview(OpenShiftUiSession session, String namespace) {
		NamespaceOverview overview = new NamespaceOverview();
		overview.setName(namespace);

		JsonNode deployments = parse(apiClient.get(session,
				"/apis/apps/v1/namespaces/" + namespace + "/deployments"));
		overview.setDeploymentCount(deployments.path("items").size());

		JsonNode pods = parse(apiClient.get(session,
				"/api/v1/namespaces/" + namespace + "/pods"));
		Map<String, Integer> podsByPhase = new LinkedHashMap<>();
		int podCount = 0;
		for (JsonNode item : iterable(pods.path("items"))) {
			podCount++;
			String phase = item.path("status").path("phase").asText("Unknown");
			podsByPhase.merge(phase, 1, Integer::sum);
		}
		overview.setPodCount(podCount);
		overview.setPodsByPhase(podsByPhase);

		JsonNode services = parse(apiClient.get(session,
				"/api/v1/namespaces/" + namespace + "/services"));
		overview.setServiceCount(services.path("items").size());

		JsonNode routes = parse(apiClient.get(session,
				"/apis/route.openshift.io/v1/namespaces/" + namespace + "/routes"));
		List<String> routeHosts = new ArrayList<>();
		for (JsonNode item : iterable(routes.path("items"))) {
			routeHosts.add(item.path("spec").path("host").asText());
		}
		overview.setRouteCount(routeHosts.size());
		overview.setRouteHosts(routeHosts);

		return overview;
	}

	public List<String> listMicroservices(OpenShiftUiSession session, String namespace) {
		JsonNode root = parse(apiClient.get(session,
				"/apis/apps/v1/namespaces/" + namespace + "/deployments"));
		List<String> names = new ArrayList<>();
		for (JsonNode item : iterable(root.path("items"))) {
			names.add(item.path("metadata").path("name").asText());
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	public MicroserviceDetail getMicroserviceDetail(
			OpenShiftUiSession session,
			String namespace,
			String microservice,
			boolean decryptSecrets) {
		String deploymentJson = apiClient.get(session,
				"/apis/apps/v1/namespaces/" + namespace + "/deployments/" + microservice);
		JsonNode deployment = parse(deploymentJson);

		MicroserviceDetail detail = new MicroserviceDetail();
		detail.setName(microservice);
		detail.setNamespace(namespace);

		int desired = deployment.path("spec").path("replicas").asInt(0);
		int available = deployment.path("status").path("availableReplicas").asInt(0);
		detail.setPodsSummary(available + " / " + desired);

		Map<String, String> selector = extractMatchLabels(deployment.path("spec").path("selector").path("matchLabels"));
		detail.setPods(listMatchingPods(session, namespace, selector));

		Set<String> configMapNames = extractConfigMapRefs(deployment);
		Set<String> secretNames = extractSecretRefs(deployment);

		detail.setConfigMaps(fetchConfigMaps(session, namespace, configMapNames));
		detail.setSecrets(fetchSecrets(session, namespace, secretNames, decryptSecrets));
		detail.setApplicationVersion(extractApplicationVersion(deployment));
		detail.setCertificates(findCertificates(session, namespace, secretNames));
		detail.setServices(findMatchingServices(session, namespace, selector));
		detail.setRoutes(findMatchingRoutes(session, namespace, detail.getServices()));
		detail.setExternalDependencies(inferDependencies(deployment, detail.getConfigMaps(), detail.getSecrets()));

		detail.setDetailRows(buildDetailRows(detail));
		return detail;
	}

	private List<DetailRow> buildDetailRows(MicroserviceDetail detail) {
		List<DetailRow> rows = new ArrayList<>();
		rows.add(new DetailRow("Running Pods", detail.getPodsSummary()));
		rows.add(new DetailRow("Pod List", formatPods(detail.getPods())));
		rows.add(new DetailRow("Application Version", nullToDash(detail.getApplicationVersion())));
		rows.add(new DetailRow("ConfigMaps", formatMapOfMaps(detail.getConfigMaps())));
		rows.add(new DetailRow("Secrets", formatMapOfMaps(detail.getSecrets())));
		rows.add(new DetailRow("Certificates", formatCertificates(detail.getCertificates())));
		rows.add(new DetailRow("Routes", detail.getRoutes().isEmpty() ? "-" : String.join(", ", detail.getRoutes())));
		rows.add(new DetailRow("Services", formatServices(detail.getServices())));
		rows.add(new DetailRow("External Dependencies",
				detail.getExternalDependencies().isEmpty() ? "-"
						: String.join(", ", detail.getExternalDependencies())));
		return rows;
	}

	private List<PodInfo> listMatchingPods(OpenShiftUiSession session, String namespace, Map<String, String> selector) {
		JsonNode root = parse(apiClient.get(session, "/api/v1/namespaces/" + namespace + "/pods"));
		List<PodInfo> pods = new ArrayList<>();
		for (JsonNode item : iterable(root.path("items"))) {
			JsonNode labels = item.path("metadata").path("labels");
			if (matchesSelector(labels, selector)) {
				pods.add(new PodInfo(
						item.path("metadata").path("name").asText(),
						item.path("status").path("phase").asText("Unknown")));
			}
		}
		return pods;
	}

	private Map<String, Map<String, String>> fetchConfigMaps(
			OpenShiftUiSession session, String namespace, Set<String> names) {
		Map<String, Map<String, String>> result = new LinkedHashMap<>();
		for (String name : names) {
			try {
				JsonNode cm = parse(apiClient.get(session,
						"/api/v1/namespaces/" + namespace + "/configmaps/" + name));
				result.put(name, extractDataMap(cm.path("data")));
			}
			catch (Exception ex) {
				result.put(name, Map.of("error", ex.getMessage()));
			}
		}
		return result;
	}

	private Map<String, Map<String, String>> fetchSecrets(
			OpenShiftUiSession session, String namespace, Set<String> names, boolean decrypt) {
		Map<String, Map<String, String>> result = new LinkedHashMap<>();
		for (String name : names) {
			try {
				JsonNode secret = parse(apiClient.get(session,
						"/api/v1/namespaces/" + namespace + "/secrets/" + name));
				result.put(name, extractSecretData(secret.path("data"), decrypt));
			}
			catch (Exception ex) {
				result.put(name, Map.of("error", ex.getMessage()));
			}
		}
		return result;
	}

	private List<CertificateInfo> findCertificates(
			OpenShiftUiSession session, String namespace, Set<String> referencedSecrets) {
		List<CertificateInfo> certs = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		for (String name : referencedSecrets) {
			addCertificateIfTls(session, namespace, name, certs, seen);
		}

		try {
			JsonNode root = parse(apiClient.get(session, "/api/v1/namespaces/" + namespace + "/secrets"));
			for (JsonNode item : iterable(root.path("items"))) {
				String name = item.path("metadata").path("name").asText();
				String type = item.path("type").asText("");
				if ("kubernetes.io/tls".equals(type)
						|| name.contains("tls") || name.contains("cert") || name.contains("ssl")) {
					addCertificateFromNode(item, certs, seen);
				}
			}
		}
		catch (Exception ignored) {
			// optional enrichment
		}
		return certs;
	}

	private void addCertificateIfTls(
			OpenShiftUiSession session, String namespace, String name,
			List<CertificateInfo> certs, Set<String> seen) {
		try {
			JsonNode secret = parse(apiClient.get(session,
					"/api/v1/namespaces/" + namespace + "/secrets/" + name));
			addCertificateFromNode(secret, certs, seen);
		}
		catch (Exception ignored) {
		}
	}

	private void addCertificateFromNode(JsonNode secret, List<CertificateInfo> certs, Set<String> seen) {
		String name = secret.path("metadata").path("name").asText();
		if (!seen.add(name)) {
			return;
		}
		String type = secret.path("type").asText("");
		JsonNode data = secret.path("data");
		if ("kubernetes.io/tls".equals(type) || data.has("tls.crt") || data.has("tls.key")) {
			List<String> keys = new ArrayList<>();
			data.propertyNames().forEach(keys::add);
			certs.add(new CertificateInfo(name, keys));
		}
	}

	private List<ServiceInfo> findMatchingServices(
			OpenShiftUiSession session, String namespace, Map<String, String> selector) {
		JsonNode root = parse(apiClient.get(session, "/api/v1/namespaces/" + namespace + "/services"));
		List<ServiceInfo> services = new ArrayList<>();
		for (JsonNode item : iterable(root.path("items"))) {
			JsonNode svcSelector = item.path("spec").path("selector");
			if (matchesSelector(svcSelector, selector)) {
				services.add(new ServiceInfo(
						item.path("metadata").path("name").asText(),
						formatPorts(item.path("spec").path("ports")),
						formatLabels(svcSelector)));
			}
		}
		return services;
	}

	private List<String> findMatchingRoutes(OpenShiftUiSession session, String namespace, List<ServiceInfo> services) {
		Set<String> serviceNames = services.stream().map(ServiceInfo::name).collect(Collectors.toSet());
		JsonNode root = parse(apiClient.get(session,
				"/apis/route.openshift.io/v1/namespaces/" + namespace + "/routes"));
		List<String> hosts = new ArrayList<>();
		for (JsonNode item : iterable(root.path("items"))) {
			String targetService = item.path("spec").path("to").path("name").asText();
			if (serviceNames.isEmpty() || serviceNames.contains(targetService)) {
				hosts.add(item.path("spec").path("host").asText());
			}
		}
		return hosts;
	}

	private Set<String> extractConfigMapRefs(JsonNode deployment) {
		Set<String> names = new LinkedHashSet<>();
		for (JsonNode container : iterable(deployment.path("spec").path("template").path("spec").path("containers"))) {
			for (JsonNode envFrom : iterable(container.path("envFrom"))) {
				if (envFrom.has("configMapRef")) {
					names.add(envFrom.path("configMapRef").path("name").asText());
				}
			}
			for (JsonNode env : iterable(container.path("env"))) {
				if (env.has("valueFrom") && env.path("valueFrom").has("configMapKeyRef")) {
					names.add(env.path("valueFrom").path("configMapKeyRef").path("name").asText());
				}
			}
		}
		for (JsonNode volume : iterable(deployment.path("spec").path("template").path("spec").path("volumes"))) {
			if (volume.has("configMap")) {
				names.add(volume.path("configMap").path("name").asText());
			}
		}
		names.remove("");
		return names;
	}

	private Set<String> extractSecretRefs(JsonNode deployment) {
		Set<String> names = new LinkedHashSet<>();
		for (JsonNode container : iterable(deployment.path("spec").path("template").path("spec").path("containers"))) {
			for (JsonNode envFrom : iterable(container.path("envFrom"))) {
				if (envFrom.has("secretRef")) {
					names.add(envFrom.path("secretRef").path("name").asText());
				}
			}
			for (JsonNode env : iterable(container.path("env"))) {
				if (env.has("valueFrom") && env.path("valueFrom").has("secretKeyRef")) {
					names.add(env.path("valueFrom").path("secretKeyRef").path("name").asText());
				}
			}
		}
		for (JsonNode volume : iterable(deployment.path("spec").path("template").path("spec").path("volumes"))) {
			if (volume.has("secret")) {
				names.add(volume.path("secret").path("secretName").asText());
			}
		}
		names.remove("");
		return names;
	}

	private String extractApplicationVersion(JsonNode deployment) {
		for (JsonNode container : iterable(deployment.path("spec").path("template").path("spec").path("containers"))) {
			String image = container.path("image").asText("");
			if (image.contains(":")) {
				String tag = image.substring(image.lastIndexOf(':') + 1);
				if (!tag.startsWith("sha256")) {
					return tag;
				}
			}
			if (image.contains("@sha256:")) {
				return "digest: " + image.substring(image.indexOf("@sha256:") + 8, Math.min(image.length(), image.indexOf("@sha256:") + 20)) + "...";
			}
		}
		return "-";
	}

	private List<String> inferDependencies(
			JsonNode deployment,
			Map<String, Map<String, String>> configMaps,
			Map<String, Map<String, String>> secrets) {
		Set<String> deps = new LinkedHashSet<>();
		collectDependencyHints(deployment.path("spec").path("template").path("spec").path("containers"), deps);
		configMaps.values().forEach(map -> map.forEach((k, v) -> addIfDependency(k + "=" + v, deps)));
		secrets.values().forEach(map -> map.keySet().forEach(k -> addIfDependency(k, deps)));
		return new ArrayList<>(deps);
	}

	private void collectDependencyHints(JsonNode containers, Set<String> deps) {
		for (JsonNode container : iterable(containers)) {
			for (JsonNode env : iterable(container.path("env"))) {
				String name = env.path("name").asText();
				String value = env.has("value") ? env.path("value").asText() : name;
				addIfDependency(name + "=" + value, deps);
			}
		}
	}

	private void addIfDependency(String text, Set<String> deps) {
		String lower = text.toLowerCase();
		for (String keyword : DEPENDENCY_KEYWORDS) {
			if (lower.contains(keyword)) {
				deps.add(keyword.toUpperCase());
			}
		}
	}

	private Map<String, String> extractMatchLabels(JsonNode matchLabels) {
		Map<String, String> labels = new LinkedHashMap<>();
		matchLabels.properties().forEach(e -> labels.put(e.getKey(), e.getValue().asText()));
		return labels;
	}

	private Map<String, String> extractDataMap(JsonNode data) {
		Map<String, String> map = new LinkedHashMap<>();
		if (data != null && data.isObject()) {
			data.properties().forEach(e -> map.put(e.getKey(), e.getValue().asText()));
		}
		return map;
	}

	private Map<String, String> extractSecretData(JsonNode data, boolean decrypt) {
		Map<String, String> map = new LinkedHashMap<>();
		if (data == null || !data.isObject()) {
			return map;
		}
		for (var entry : data.properties()) {
			String encoded = entry.getValue().asText("");
			if (decrypt) {
				map.put(entry.getKey(), decodeBase64(encoded));
			}
			else {
				map.put(entry.getKey(), "[encrypted - click Decrypt to reveal]");
			}
		}
		return map;
	}

	private String decodeBase64(String value) {
		try {
			return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
		}
		catch (Exception ex) {
			return value;
		}
	}

	private boolean matchesSelector(JsonNode labels, Map<String, String> selector) {
		if (selector.isEmpty()) {
			return false;
		}
		for (Map.Entry<String, String> entry : selector.entrySet()) {
			if (!entry.getValue().equals(labels.path(entry.getKey()).asText(null))) {
				return false;
			}
		}
		return true;
	}

	private String formatPods(List<PodInfo> pods) {
		if (pods.isEmpty()) {
			return "-";
		}
		return pods.stream()
				.map(p -> p.name() + " (" + p.phase() + ")")
				.collect(Collectors.joining(", "));
	}

	private String formatMapOfMaps(Map<String, Map<String, String>> data) {
		if (data.isEmpty()) {
			return "-";
		}
		StringBuilder sb = new StringBuilder();
		data.forEach((name, entries) -> {
			sb.append(name).append(": ");
			if (entries.isEmpty()) {
				sb.append("(empty)");
			}
			else {
				sb.append(entries.entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.joining(", ")));
			}
			sb.append("\n");
		});
		return sb.toString().trim();
	}

	private String formatCertificates(List<CertificateInfo> certs) {
		if (certs.isEmpty()) {
			return "-";
		}
		return certs.stream()
				.map(c -> c.name() + " [" + String.join(", ", c.keys()) + "]")
				.collect(Collectors.joining("\n"));
	}

	private String formatServices(List<ServiceInfo> services) {
		if (services.isEmpty()) {
			return "-";
		}
		return services.stream()
				.map(s -> s.name() + " ports=" + s.ports() + " selector=" + s.selector())
				.collect(Collectors.joining("\n"));
	}

	private String formatPorts(JsonNode ports) {
		List<String> parts = new ArrayList<>();
		for (JsonNode port : iterable(ports)) {
			parts.add(port.path("port").asText() + "/" + port.path("protocol").asText("TCP"));
		}
		return String.join(", ", parts);
	}

	private String formatLabels(JsonNode labels) {
		List<String> parts = new ArrayList<>();
		labels.properties().forEach(e -> parts.add(e.getKey() + "=" + e.getValue().asText()));
		return String.join(", ", parts);
	}

	private Iterable<JsonNode> iterable(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		return node;
	}

	private JsonNode parse(String json) {
		try {
			return objectMapper.readTree(json);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to parse OpenShift response: " + ex.getMessage());
		}
	}

	private String nullToDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
