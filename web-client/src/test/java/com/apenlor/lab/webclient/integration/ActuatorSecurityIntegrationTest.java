package com.apenlor.lab.webclient.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests focused specifically on the actuator security filter chain.
 * This test uses a running server environment to validate real HTTP requests and
 * Basic Authentication, simulating a machine-to-machine client.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Actuator Security Integration Tests")
class ActuatorSecurityIntegrationTest {

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /actuator/health - Should be publicly accessible without authentication")
    void actuatorHealth_isPubliclyAccessible() {
        String url = "http://localhost:" + managementPort + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /actuator/prometheus - Should return 401 UNAUTHORIZED for unauthenticated requests")
    void prometheusEndpoint_isSecuredFromAnonymousAccess() {
        String url = "http://localhost:" + managementPort + "/actuator/prometheus";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /actuator/prometheus - Should return 200 OK for valid credentials")
    void prometheusEndpoint_isAccessibleWithValidCredentials() {
        String url = "http://localhost:" + managementPort + "/actuator/prometheus";
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("test-actuator", "test-password")
                .getForEntity(url, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}