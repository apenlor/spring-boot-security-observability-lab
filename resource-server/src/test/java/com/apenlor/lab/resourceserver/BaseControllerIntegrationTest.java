// File: src/test/java/com/apenlor/lab/resourceserver/BaseControllerIntegrationTest.java

package com.apenlor.lab.resourceserver;

import com.apenlor.lab.resourceserver.dto.LoginRequest;
import com.apenlor.lab.resourceserver.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An abstract base class for all controller integration tests.
 * This class centralizes all common Spring Test context configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties",
        properties = {
                "JWT_SECRET_KEY=${test.jwt.secret-key}",
                "ACTUATOR_USERNAME=${test.actuator.username}",
                "ACTUATOR_PASSWORD=${test.actuator.password}",
                "ACTUATOR_ROLES=${test.actuator.roles}"
        })
public abstract class BaseControllerIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String obtainValidJwt() throws Exception {
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
}