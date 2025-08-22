package com.apenlor.lab.resourceserver.demo;

import com.apenlor.lab.resourceserver.BaseControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the ChaosController.
 * <p>
 * Architectural Note:
 * This test class uses @ActiveProfiles("chaos") to ensure that the ChaosController
 * and its dependencies are loaded into the application context.
 * It uses @MockBean to replace the real RandomnessProvider with a mock, allowing us
 * to write 100% deterministic tests for behavior that is normally random.
 */
@ActiveProfiles("chaos")
@DisplayName("Demo: /demo Endpoint (Chaos Profile)")
class ChaosControllerTest extends BaseControllerIntegrationTest {

    // By using @MockitoBean, Spring's test context will replace the real
    // RandomnessProvider bean with this mock instance wherever it is injected.
    @MockitoBean
    private RandomnessProvider randomnessProvider;

    private String validJwt;

    /**
     * Obtains a valid JWT before each test, as the demo endpoint is secured.
     */
    @BeforeEach
    void setUp() throws Exception {
        this.validJwt = obtainValidJwt();
    }

    @Test
    @DisplayName("Given a provider that dictates success, should return 200 OK")
    void getFlakyRequest_whenProviderSucceeds_shouldReturn200() throws Exception {
        // Arrange
        when(randomnessProvider.nextInt(300)).thenReturn(100);
        // Configure the mock to force a successful outcome.
        when(randomnessProvider.nextInt(5)).thenReturn(1);

        //  Act & Assert 
        mockMvc.perform(get("/demo/flaky-request")
                        .header("Authorization", "Bearer " + validJwt))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given a provider that dictates failure, should return 500 Internal Server Error")
    void getFlakyRequest_whenProviderFails_shouldReturn500() throws Exception {
        // Arrange
        when(randomnessProvider.nextInt(300)).thenReturn(100);
        // Configure the mock to force a failure outcome.
        when(randomnessProvider.nextInt(5)).thenReturn(0);

        // Act & Assert
        mockMvc.perform(get("/demo/flaky-request")
                        .header("Authorization", "Bearer " + validJwt))
                .andExpect(status().isInternalServerError());
    }
}