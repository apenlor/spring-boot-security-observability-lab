package com.apenlor.lab.resourceserver.controller;

import com.apenlor.lab.resourceserver.dto.LoginRequest;
import com.apenlor.lab.resourceserver.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the ApiController.
 * Verifies the behavior of public and secure endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void whenGetPublicInfo_thenReturns200OK() throws Exception {
        mockMvc.perform(get("/api/public/info"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is PUBLIC information. Anyone can see this."));
    }

    @Test
    void whenGetSecureDataUnauthenticated_thenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/secure/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGetSecureDataAuthenticated_thenReturns200OK() throws Exception {
        // Step 1: Authenticate to get a token
        var loginRequest = new LoginRequest("user", "password");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Step 2: Extract token from the response
        String responseBody = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);
        String token = loginResponse.jwtToken();

        // Step 3: Use the token to access the secure endpoint
        mockMvc.perform(get("/api/secure/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("This is SECURE data for user: user. You should only see this if you are authenticated."));
    }
}