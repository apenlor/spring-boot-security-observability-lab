package com.apenlor.lab.webclient.controller;

import com.apenlor.lab.aspects.audit.AuditLogAspect;
import com.apenlor.lab.webclient.config.SecurityConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebController.class)
@Import(SecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @MockitoBean
    private AuditLogAspect auditLogAspect;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void setUp() throws Throwable {
        // Configure the mock aspect to allow the actual method to be called, ignoring the @Auditable
        when(auditLogAspect.audit(any(ProceedingJoinPoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProceedingJoinPoint.class).proceed());
    }


    @Test
    @DisplayName("GET / should return index page for unauthenticated users")
    void index_unauthenticated_returnsIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /dashboard should redirect unauthenticated users to login")
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                // In a @WebMvcTest slice, the default redirect is to /login, not the full OIDC flow.
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("GET /dashboard should return dashboard with user info for authenticated users")
    void dashboard_authenticated_returnsDashboardWithUserInfo() throws Exception {
        OidcUser mockUser = createMockOidcUser();

        mockMvc.perform(get("/dashboard").with(oidcLogin().oidcUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("username", "test-user"))
                .andExpect(model().attributeExists("claims"));
    }

    @Test
    @DisplayName("GET /fetch-data should return dashboard with data on successful API call")
    void fetchData_onSuccess_returnsDashboardWithData() throws Exception {
        OidcUser mockUser = createMockOidcUser();
        String expectedData = "Secure Data from Backend";

        when(webClient.get().uri(anyString()).retrieve().bodyToMono(String.class))
                .thenReturn(Mono.just(expectedData));

        mockMvc.perform(get("/fetch-data").with(oidcLogin().oidcUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("secureData", expectedData));
    }

    @Test
    @DisplayName("GET /fetch-data should return dashboard with error on failed API call")
    void fetchData_onApiError_returnsDashboardWithError() throws Exception {
        OidcUser mockUser = createMockOidcUser();
        var exception = WebClientResponseException.create(500, "Internal Server Error", null, null, null);

        when(webClient.get().uri(anyString()).retrieve().bodyToMono(String.class))
                .thenReturn(Mono.error(exception));

        mockMvc.perform(get("/fetch-data").with(oidcLogin().oidcUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("secureData", containsString("500 INTERNAL_SERVER_ERROR")));
    }

    @Test
    @DisplayName("GET /fetch-admin-data should return dashboard with error on 403 Forbidden API call")
    void fetchAdminData_onApiForbidden_returnsDashboardWithForbiddenError() throws Exception {
        OidcUser mockUser = createMockOidcUser();
        var exception = WebClientResponseException.create(403, "Forbidden", null, null, null);

        when(webClient.get().uri(anyString()).retrieve().bodyToMono(String.class))
                .thenReturn(Mono.error(exception));

        mockMvc.perform(get("/fetch-admin-data").with(oidcLogin().oidcUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("adminData", containsString("403 FORBIDDEN")));
    }

    @Test
    @DisplayName("POST /trigger-5xx-error should redirect to dashboard with success flash attribute")
    void trigger5xxError_shouldRedirectWithSuccessFlashAttribute() throws Exception {
        OidcUser mockUser = createMockOidcUser();

        // We simulate a successful call that is handled by the controller.
        when(webClient.get().uri(anyString()).retrieve().toBodilessEntity())
                .thenReturn(Mono.empty()); // Assume the call completes, even if it's an error handled internally.

        mockMvc.perform(post("/trigger-5xx-error")
                        .with(oidcLogin().oidcUser(mockUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("feedbackMessage", "Successfully triggered a 5xx error in the resource-server."))
                .andExpect(flash().attribute("isError", false));
    }

    @Test
    @DisplayName("POST /trigger-403-error should redirect to dashboard with success flash attribute on 4xx error")
    void trigger403Error_on4xxError_shouldRedirectWithSuccessFlashAttribute() throws Exception {
        OidcUser mockUser = createMockOidcUser();

        // Simulate the WebClient throwing a 403 Forbidden exception.
        var exception = new WebClientResponseException(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                HttpHeaders.EMPTY,
                null,
                null
        );

        when(webClient.get().uri(anyString()).retrieve().toBodilessEntity())
                .thenReturn(Mono.error(exception));

        mockMvc.perform(post("/trigger-403-error")
                        .with(oidcLogin().oidcUser(mockUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("feedbackMessage", "Successfully triggered a 403 FORBIDDEN error from the resource-server."))
                .andExpect(flash().attribute("isError", false));
    }

    /**
     * Creates a mock OidcUser for use in Spring Security tests.
     * Ensures all mandatory claims required by the OidcIdToken constructor are present.
     *
     * @return A fully-formed OidcUser object.
     */
    private OidcUser createMockOidcUser() {
        Map<String, Object> allClaims = new HashMap<>();
        // Add mandatory claims required by the OidcIdToken constructor and the spec.
        allClaims.putIfAbsent("sub", "1234567890");
        allClaims.putIfAbsent("iss", "https://test-issuer.com");
        allClaims.putIfAbsent("aud", "test-client-id");
        allClaims.putIfAbsent("preferred_username", "test-user");

        OidcIdToken idToken = new OidcIdToken(
                "test-token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                allClaims
        );

        return new DefaultOidcUser(
                Collections.singletonList(new OAuth2UserAuthority(allClaims)),
                idToken,
                "preferred_username"
        );
    }
}