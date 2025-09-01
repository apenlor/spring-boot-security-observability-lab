package com.apenlor.lab.webclient.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests focused on the user-facing OIDC security flow and WebController logic.
 * This test uses MockMvc to simulate requests and mocks the OIDC login process, which is
 * the standard practice for testing OIDC-secured endpoints without a live identity provider.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Web Controller OIDC Flow Tests")
class OidcSecurityIntegrationTest {

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebClient webClient;

    // Mock WebClient internals for chaining
    @MockitoBean
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @MockitoBean
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @MockitoBean
    private WebClient.ResponseSpec responseSpec;


    private OidcUser mockOidcUser;

    @BeforeEach
    void setUp() {
        mockOidcUser = createMockOidcUser();

        // General setup for mocked WebClient chain
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec)
                .uri("http://mock-resource-server/api/secure/data");
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec)
                .uri("http://mock-resource-server/api/secure/admin");
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    }

    @Test
    @DisplayName("GET / - Anonymous access should be permitted and return index view")
    void index_whenAnonymous_shouldSucceed() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /dashboard - Unauthenticated access should redirect to OIDC login")
    void dashboard_whenUnauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                // In a @AutoConfigureMockMvc slice, the default redirect is to /login, not the full OIDC flow.
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("GET /dashboard - Authenticated access with OIDC user should return dashboard view")
    void dashboard_whenAuthenticated_shouldSucceed() throws Exception {
        mockMvc.perform(get("/dashboard").with(oidcLogin().oidcUser(mockOidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("username", "test-user"))
                .andExpect(model().attributeExists("claims"));
    }

    @Test
    @DisplayName("GET /fetch-data - Authenticated user should successfully fetch data")
    void fetchData_whenAuthenticated_shouldReturnData() throws Exception {
        String mockData = "Secure data from backend";
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockData));

        mockMvc.perform(get("/fetch-data").with(oidcLogin().oidcUser(mockOidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("secureData", mockData));
    }

    @Test
    @DisplayName("GET /fetch-data - When backend returns error, should display error message")
    void fetchData_whenBackendFails_shouldShowErrorMessage() throws Exception {
        WebClientResponseException error = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error", null, "Internal Server Error".getBytes(), null);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(error));

        mockMvc.perform(get("/fetch-data").with(oidcLogin().oidcUser(mockOidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("secureData", "Error fetching data: 500 INTERNAL_SERVER_ERROR - Internal Server Error"));
    }

    private OidcUser createMockOidcUser() {
        Map<String, Object> claims = Map.of(
                "sub", "123456789", "name", "Test User", "preferred_username", "test-user", "email", "test-user" + "@example.com"
        );
        OidcIdToken idToken = new OidcIdToken("mock-token", Instant.now(), Instant.now().plusSeconds(60), claims);
        return new DefaultOidcUser(null, idToken);
    }
}