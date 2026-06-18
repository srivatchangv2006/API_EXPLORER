package com.srivatchan.openshiftui.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srivatchan.openshiftui.config.OpenShiftClusterProperties;
import com.srivatchan.openshiftui.exception.OpenShiftApiException;
import com.srivatchan.openshiftui.service.LoginCommandParser;
import com.srivatchan.openshiftui.service.LoginCommandParser.ParsedLoginCommand;
import com.srivatchan.openshiftui.service.OpenShiftUiService;
import com.srivatchan.openshiftui.session.OpenShiftUiSession;

@Controller
@RequestMapping("/openshift-ui")
public class OpenShiftUiController {

	public static final String SESSION_KEY = "openshiftUiSession";

	private final OpenShiftClusterProperties clusterProperties;
	private final LoginCommandParser loginCommandParser;
	private final OpenShiftUiService openShiftUiService;

	public OpenShiftUiController(
			OpenShiftClusterProperties clusterProperties,
			LoginCommandParser loginCommandParser,
			OpenShiftUiService openShiftUiService) {
		this.clusterProperties = clusterProperties;
		this.loginCommandParser = loginCommandParser;
		this.openShiftUiService = openShiftUiService;
	}

	@GetMapping({"", "/"})
	public String home() {
		return "ui/cluster";
	}

	@PostMapping("/connect")
	public String connect(@RequestParam String cluster, HttpSession session, RedirectAttributes redirectAttributes) {
		if (clusterProperties.findCluster(cluster).isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Unknown cluster: " + cluster);
			return "redirect:/openshift-ui";
		}

		OpenShiftUiSession uiSession = getOrCreateSession(session);
		uiSession.setCluster(cluster.trim());
		uiSession.setServer(null);
		uiSession.setToken(null);
		session.setAttribute(SESSION_KEY, uiSession);
		return "redirect:/openshift-ui/login";
	}

	@GetMapping("/login")
	public String loginPage(HttpSession session, Model model) {
		OpenShiftUiSession uiSession = getSession(session);
		if (uiSession == null || uiSession.getCluster() == null) {
			return "redirect:/openshift-ui";
		}
		var clusterInfo = clusterProperties.findCluster(uiSession.getCluster());
		if (clusterInfo.isEmpty()) {
			return "redirect:/openshift-ui";
		}
		model.addAttribute("cluster", uiSession.getCluster());
		model.addAttribute("consoleUrl", clusterInfo.get().getConsoleUrl());
		return "ui/login";
	}

	@GetMapping("/paste")
	public String pastePage(HttpSession session, Model model) {
		OpenShiftUiSession uiSession = getSession(session);
		if (uiSession == null || uiSession.getCluster() == null) {
			return "redirect:/openshift-ui";
		}
		model.addAttribute("cluster", uiSession.getCluster());
		return "ui/paste";
	}

	@PostMapping("/paste")
	public String pasteCommand(
			@RequestParam String loginCommand,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		OpenShiftUiSession uiSession = getSession(session);
		if (uiSession == null || uiSession.getCluster() == null) {
			return "redirect:/openshift-ui";
		}
		try {
			ParsedLoginCommand parsed = loginCommandParser.parse(loginCommand);
			uiSession.setToken(parsed.token());
			uiSession.setServer(parsed.server());
			session.setAttribute(SESSION_KEY, uiSession);
			return "redirect:/openshift-ui/namespaces";
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/openshift-ui/paste";
		}
	}

	@GetMapping("/namespaces")
	public String namespaces(HttpSession session, Model model) {
		OpenShiftUiSession uiSession = getSession(session);
		if (uiSession == null || !uiSession.isConnected()) {
			return "redirect:/openshift-ui/paste";
		}
		try {
			model.addAttribute("cluster", uiSession.getCluster());
			model.addAttribute("namespaces", openShiftUiService.listNamespaces(uiSession));
			return "ui/namespaces";
		}
		catch (OpenShiftApiException ex) {
			model.addAttribute("error", "Failed to load projects: " + ex.getMessage());
			model.addAttribute("cluster", uiSession.getCluster());
			return "ui/paste";
		}
	}

	@GetMapping("/namespaces/{namespace}")
	public String microservices(
			@PathVariable String namespace,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {
		OpenShiftUiSession uiSession = getSession(session);
		if (uiSession == null || !uiSession.isConnected()) {
			return "redirect:/openshift-ui/paste";
		}
		try {
			model.addAttribute("cluster", uiSession.getCluster());
			model.addAttribute("namespace", namespace);
			model.addAttribute("overview", openShiftUiService.getNamespaceOverview(uiSession, namespace));
			model.addAttribute("microservices", openShiftUiService.listMicroservices(uiSession, namespace));
			return "ui/microservices";
		}
		catch (OpenShiftApiException ex) {
			redirectAttributes.addFlashAttribute("error", "Failed to load microservices: " + ex.getMessage());
			return "redirect:/openshift-ui/namespaces";
		}
	}

	@GetMapping("/namespaces/{namespace}/microservices/{name}")
	public String microserviceDetail(
			@PathVariable String namespace,
			@PathVariable String name,
			@RequestParam(defaultValue = "false") boolean decrypt,
			HttpSession session,
			Model model) {
		OpenShiftUiSession uiSession = getSession(session);
		if (uiSession == null || !uiSession.isConnected()) {
			return "redirect:/openshift-ui/paste";
		}
		try {
			model.addAttribute("cluster", uiSession.getCluster());
			model.addAttribute("namespace", namespace);
			model.addAttribute("decrypt", decrypt);
			model.addAttribute("detail", openShiftUiService.getMicroserviceDetail(uiSession, namespace, name, decrypt));
			return "ui/microservice-detail";
		}
		catch (OpenShiftApiException ex) {
			redirectAttributesOnMicroserviceError(model, uiSession, namespace, ex);
			return "ui/microservices";
		}
	}

	@PostMapping("/disconnect")
	public String disconnect(HttpSession session) {
		session.removeAttribute(SESSION_KEY);
		return "redirect:/openshift-ui";
	}

	private void redirectAttributesOnMicroserviceError(
			Model model, OpenShiftUiSession uiSession, String namespace, OpenShiftApiException ex) {
		model.addAttribute("error", "Failed to load microservice: " + ex.getMessage());
		model.addAttribute("cluster", uiSession.getCluster());
		model.addAttribute("namespace", namespace);
		try {
			model.addAttribute("microservices", openShiftUiService.listMicroservices(uiSession, namespace));
		}
		catch (Exception ignored) {
			model.addAttribute("microservices", java.util.List.of());
		}
	}

	private OpenShiftUiSession getSession(HttpSession session) {
		return (OpenShiftUiSession) session.getAttribute(SESSION_KEY);
	}

	private OpenShiftUiSession getOrCreateSession(HttpSession session) {
		OpenShiftUiSession uiSession = getSession(session);
		if (uiSession == null) {
			uiSession = new OpenShiftUiSession();
		}
		return uiSession;
	}
}
