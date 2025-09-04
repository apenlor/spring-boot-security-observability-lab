package com.apenlor.lab.resourceserver.api;

import com.apenlor.lab.resourceserver.BaseControllerIntegrationTest;
import com.apenlor.lab.resourceserver.util.RandomnessProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the ChaosController.
 * <p>
 * This test class uses @ActiveProfiles("chaos") to ensure that the ChaosController
 * and its dependencies are loaded into the application context.
 * It uses @MockBean to replace the real RandomnessProvider with a mock, allowing us
 * to write deterministic tests for behavior that is normally random.
 */
@ActiveProfiles("chaos")
@DisplayName("API: /api/chaos Endpoints (Integration Test)")
class ChaosControllerTest extends BaseControllerIntegrationTest {

    @MockitoBean
    private RandomnessProvider randomnessProvider;

    @Test
    @DisplayName("GET /api/chaos/error should always return 500 Internal Server Error")
    void getGuaranteedError_shouldAlwaysReturn500() throws Exception {
        mockMvc.perform(get("/api/chaos/error").with(jwt()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/chaos/flaky-request: Given a provider that dictates success, should return 200 OK")
    void getFlakyRequest_whenProviderSucceeds_shouldReturn200() throws Exception {
        when(randomnessProvider.nextInt(5)).thenReturn(1); // Any non-zero value

        mockMvc.perform(get("/api/chaos/flaky-request").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/chaos/flaky-request: Given a provider that dictates failure, should return 500 Internal Server Error")
    void getFlakyRequest_whenProviderFails_shouldReturn500() throws Exception {
        when(randomnessProvider.nextInt(5)).thenReturn(0);

        mockMvc.perform(get("/api/chaos/flaky-request").with(jwt()))
                .andExpect(status().isInternalServerError());
    }
}