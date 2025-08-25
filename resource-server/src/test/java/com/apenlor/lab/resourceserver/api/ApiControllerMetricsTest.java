package com.apenlor.lab.resourceserver.api;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the metric instrumentation within the ApiController.
 * <p>
 * This test verifies that custom business metrics defined in the controller are
 * incremented correctly under the expected conditions. It uses a SimpleMeterRegistry
 * to test the metrics logic in a fast and isolated manner.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("API Controller Metrics Unit Tests")
class ApiControllerMetricsTest {

    @Mock
    private Authentication mockAuthentication;

    private Counter secureEndpointRequestCounter;
    private ApiController apiController;

    @BeforeEach
    void setUp() {
        // Use a real, in-memory MeterRegistry for testing.
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        // Manually create the Counter bean, just as MetricsConfig would.
        this.secureEndpointRequestCounter = Counter.builder("api.requests.secure.total")
                .description("Total number of requests to the secure data endpoint.")
                .tag("endpoint", "/api/secure/data")
                .register(meterRegistry);

        // Instantiate the controller with its real dependency (the counter).
        apiController = new ApiController(secureEndpointRequestCounter);
    }

    @Test
    @DisplayName("When getPublicInfo is called, should NOT increment the secure request counter")
    void getPublicInfo_shouldNotIncrementSecureRequestCounter() {
        // Arrange
        assertEquals(0.0, secureEndpointRequestCounter.count(), "Counter should start at 0");

        // Act
        apiController.getPublicInfo();

        // Assert
        assertEquals(0.0, secureEndpointRequestCounter.count(), "Counter should not be incremented for public endpoint");
    }

    @Test
    @DisplayName("When getSecureData is called, should increment the secure request counter")
    void getSecureData_shouldIncrementSecureRequestCounter() {
        // Arrange
        when(mockAuthentication.getName()).thenReturn("test-user");

        // Initial state check
        assertEquals(0.0, secureEndpointRequestCounter.count(), "Counter should start at 0");

        // Act
        apiController.getSecureData(mockAuthentication);

        // Assert
        assertEquals(1.0, secureEndpointRequestCounter.count(), "Counter should be incremented by 1");
    }

    @Test
    @DisplayName("When getAdminData is called, should increment the secure request counter")
    void getAdminData_shouldIncrementSecureRequestCounter() {
        // Arrange
        when(mockAuthentication.getName()).thenReturn("admin-user");
        assertEquals(0.0, secureEndpointRequestCounter.count(), "Counter should start at 0");

        // Act
        apiController.getAdminData(mockAuthentication);

        // Assert
        assertEquals(1.0, secureEndpointRequestCounter.count(), "Counter should be incremented by 1 for admin endpoint");
    }
}