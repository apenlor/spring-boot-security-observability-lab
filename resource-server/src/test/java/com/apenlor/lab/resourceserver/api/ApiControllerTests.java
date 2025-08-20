package com.apenlor.lab.resourceserver.api;

import com.apenlor.lab.resourceserver.dto.ApiResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the business API endpoints (/api).
 * These tests verify the correct application of security rules to public and protected resources,
 * including a full authentication and token-based access flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API: /api Endpoints")
class ApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * A private helper method to perform a login and extract the resulting JWT.
     *
     * @return A valid JWT string for the test user.
     * @throws Exception if the mockMvc performance fails.
     */
    private String obtainValidJwt() throws Exception {
        var loginRequest = new LoginRequest("user", "password");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);
        return loginResponse.jwtToken();
    }

    @Nested
    @DisplayName("GET /public/info")
    class PublicEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with the correct public message")
        void getPublicInfo_shouldReturn200() throws Exception {
            // Act
            MvcResult result = mockMvc.perform(get("/api/public/info"))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert by deserializing to the DTO.
            ApiResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);
            assertEquals("This is PUBLIC information. Anyone can see this.", response.message());
        }
    }

    @Nested
    @DisplayName("GET /secure/data")
    class SecureEndpointTests {

        @Test
        @DisplayName("Given no token, should return 401 Unauthorized")
        void getSecureData_withoutToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/secure/data"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Given a valid token, should return 200 OK with personalized data")
        void getSecureData_withValidToken_shouldReturn200() throws Exception {
            // Arrange: Obtain a valid token.
            String token = obtainValidJwt();

            // Act
            MvcResult result = mockMvc.perform(get("/api/secure/data")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert by deserializing to the DTO.
            ApiResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);
            assertEquals("This is SECURE data for user: user. You should only see this if you are authenticated.", response.message());
        }
    }
}