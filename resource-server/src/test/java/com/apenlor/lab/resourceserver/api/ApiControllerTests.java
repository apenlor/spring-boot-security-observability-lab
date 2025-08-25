package com.apenlor.lab.resourceserver.api;

import com.apenlor.lab.resourceserver.BaseControllerIntegrationTest;
import com.apenlor.lab.resourceserver.config.SecurityConfig;
import com.apenlor.lab.resourceserver.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API: /api Endpoints")
@Import(SecurityConfig.class)
class ApiControllerTests extends BaseControllerIntegrationTest {

    @Nested
    @DisplayName("GET /public/info")
    class PublicEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with the correct public message")
        void getPublicInfo_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/public/info"))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);
            assertEquals("This is PUBLIC information. Anyone can see this.", response.message());
        }
    }

    @Nested
    @DisplayName("GET /secure/data")
    class SecureDataEndpointTests {

        @Test
        @DisplayName("Given no token, should return 401 Unauthorized")
        void getSecureData_withoutToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/secure/data"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Given a valid mock JWT, should return 200 OK with personalized data")
        void getSecureData_withValidJwt_shouldReturn200() throws Exception {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor mockJwt = jwt()
                    .jwt(jwt -> jwt.subject("test-user"));

            MvcResult result = mockMvc.perform(get("/api/secure/data").with(mockJwt))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);
            assertEquals("This is SECURE data for user: test-user. You should only see this if you are authenticated.", response.message());
        }
    }

    @Nested
    @DisplayName("GET /secure/admin")
    class SecureAdminEndpointTests {

        @Test
        @DisplayName("Given no token, should return 401 Unauthorized")
        void getAdminData_withoutToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/secure/admin"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Given a JWT without ADMIN role, should return 403 Forbidden")
        void getAdminData_withJwtWithoutAdminRole_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/secure/admin").with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Given a JWT with ADMIN role, should return 200 OK")
        void getAdminData_withJwtWithAdminRole_shouldReturn200() throws Exception {
            mockMvc.perform(get("/api/secure/admin")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());
        }

    }
}