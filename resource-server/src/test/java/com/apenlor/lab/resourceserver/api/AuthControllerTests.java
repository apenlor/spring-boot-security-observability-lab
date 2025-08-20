package com.apenlor.lab.resourceserver.api;

import com.apenlor.lab.resourceserver.dto.LoginRequest;
import com.apenlor.lab.resourceserver.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the authentication endpoint (/auth).
 * <p>
 * These tests use @SpringBootTest to load the full application context, ensuring that
 * the entire security filter chain and the custom JwtAuthenticationProvider are active.
 * This validates the complete login flow from HTTP request to JWT response.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API: /auth Endpoint")
@TestPropertySource(properties = {
        "JWT_SECRET_KEY=a-valid-secret-key-for-testing-that-is-at-least-32-bytes-long"
})
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Given valid credentials, should return 200 OK with a non-empty JWT")
        void login_withValidCredentials_shouldReturn200AndJwt() throws Exception {
            // Arrange
            var loginRequest = new LoginRequest("user", "password");

            // Act
            MvcResult result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String responseBody = result.getResponse().getContentAsString();
            LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);
            assertNotNull(loginResponse.jwtToken(), "JWT token should not be null");
            assertTrue(loginResponse.jwtToken().length() > 50, "JWT token should be a substantial string");
        }

        @Test
        @DisplayName("Given invalid credentials, should return 401 Unauthorized")
        void login_withInvalidCredentials_shouldReturn401() throws Exception {
            // Arrange
            var loginRequest = new LoginRequest("user", "wrong-password");

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }
}