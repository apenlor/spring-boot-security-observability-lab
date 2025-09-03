package com.apenlor.lab.webclient.controller;

import com.apenlor.lab.aspects.audit.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Main controller for the web application.
 * Handles rendering of the UI and orchestrates secure, service-to-service calls
 * to the backend resource server.
 */
@Controller
@Slf4j
public class WebController {

    public static final String DASHBOARD_TEMPLATE = "dashboard";
    private final WebClient webClient;

    @Value("${resource-server.url}")
    private String resourceServerUrl;

    public WebController(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Renders the public home page. This endpoint is accessible to anyone.
     *
     * @return The view name for the home page (templates/index.html).
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Renders the protected user dashboard.
     * <p>
     * Architectural Note:
     * Accessing this endpoint without an active session will trigger Spring Security's
     * OAuth2 client support, which automatically initiates the Authorization Code Grant
     * flow by redirecting the user's browser to the configured provider (Keycloak).
     *
     * @param model    The Spring MVC model, used to pass data to the view.
     * @param oidcUser The authenticated user principal, injected by Spring Security.
     *                 This object contains the user's details and all claims from the ID Token.
     * @return The view name for the dashboard (templates/dashboard.html).
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        populateModelWithUserDetails(model, oidcUser);
        return DASHBOARD_TEMPLATE;
    }

    /**
     * An endpoint that triggers a secure backend API call to fetch general data.
     * This demonstrates a standard, authenticated service-to-service request.
     *
     * @param model    The Spring MVC model to pass data to the view.
     * @param oidcUser The authenticated principal for the current session.
     * @return The view name for the dashboard, populated with the fetched data or an error message.
     */
    @GetMapping("/fetch-data")
    @Auditable
    public String fetchData(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        populateModelWithUserDetails(model, oidcUser);
        try {
            log.info("User '{}' fetching secure data from resource-server...", oidcUser.getPreferredUsername());
            String secureData = webClient.get()
                    .uri(resourceServerUrl + "/api/secure/data")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // block() is used here for simplicity in a traditional MVC controller.

            model.addAttribute("secureData", secureData);
            log.info("Successfully fetched secure data for user '{}'.", oidcUser.getPreferredUsername());

        } catch (WebClientResponseException e) {
            String errorMessage = String.format("Error fetching data: %s - %s", e.getStatusCode(), e.getResponseBodyAsString());
            log.error("Failed to fetch secure data for user '{}'. Reason: {}", oidcUser.getPreferredUsername(), errorMessage);
            model.addAttribute("secureData", errorMessage);
        }
        return DASHBOARD_TEMPLATE;
    }

    /**
     * An endpoint that triggers a secure backend API call to fetch admin-only data.
     * This demonstrates a role-based, authorized service-to-service request.
     *
     * @param model    The Spring MVC model to pass data to the view.
     * @param oidcUser The authenticated principal for the current session.
     * @return The view name for the dashboard, populated with admin data or a 403 Forbidden error message.
     */
    @GetMapping("/fetch-admin-data")
    @Auditable
    public String fetchAdminData(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        populateModelWithUserDetails(model, oidcUser);
        try {
            log.info("User '{}' fetching ADMIN data from resource-server...", oidcUser.getPreferredUsername());
            String adminData = webClient.get()
                    .uri(resourceServerUrl + "/api/secure/admin")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            model.addAttribute("adminData", adminData);
            log.info("Successfully fetched ADMIN data for user '{}'.", oidcUser.getPreferredUsername());

        } catch (WebClientResponseException e) {
            String errorMessage = String.format("Error fetching admin data: %s - %s", e.getStatusCode(), e.getResponseBodyAsString());
            log.error("Failed to fetch ADMIN data for user '{}'. Reason: {}", oidcUser.getPreferredUsername(), errorMessage);
            model.addAttribute("adminData", errorMessage);
        }
        return DASHBOARD_TEMPLATE;
    }

    /**
     * A private helper method to ensure user details are consistently added to the model.
     * This prevents the user's name and claims from disappearing from the UI after an
     * action (like fetching data) re-renders the page.
     *
     * @param model    The Spring MVC model.
     * @param oidcUser The authenticated principal.
     */
    private void populateModelWithUserDetails(Model model, OidcUser oidcUser) {
        if (oidcUser != null) {
            model.addAttribute("username", oidcUser.getPreferredUsername());
            model.addAttribute("claims", oidcUser.getClaims());
        }
    }
}