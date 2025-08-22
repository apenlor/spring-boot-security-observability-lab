package com.apenlor.lab.resourceserver.api.metrics;

import com.apenlor.lab.resourceserver.api.AuthController;
import com.apenlor.lab.resourceserver.auth.service.JwtTokenService;
import com.apenlor.lab.resourceserver.dto.LoginRequest;
import com.apenlor.lab.resourceserver.exception.GlobalExceptionHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the metric instrumentation within the AuthController.
 * <p>
 * Architectural Note:
 * This is a focused unit test, not an integration test. It uses Mockito to isolate the
 * controller from its dependencies and a SimpleMeterRegistry to test the metrics logic
 * in a fast, reliable, and in-memory way. This ensures we are testing only the
 * controller's instrumentation logic, not the entire authentication stack.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Metrics Unit Tests")
class AuthControllerMetricsTest {

    @Mock
    private AuthenticationProvider jwtAuthenticationProvider;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private Authentication successfulAuthentication;

    @Mock
    private HttpServletRequest mockRequest;

    private Counter successfulLoginsCounter;
    private Counter failedLoginsCounter;
    private GlobalExceptionHandler globalExceptionHandler;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // Use a real, in-memory MeterRegistry for testing.
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        // Manually create the Counter beans, just as MetricsConfig would.
        this.successfulLoginsCounter = Counter.builder("auth.logins.total")
                .tag("result", "success").register(meterRegistry);

        this.failedLoginsCounter = Counter.builder("auth.logins.total")
                .tag("result", "failure").register(meterRegistry);

        // Instantiate the controller with its real dependencies (the counters) and mocks.
        authController = new AuthController(jwtAuthenticationProvider, jwtTokenService, successfulLoginsCounter);

        // Instantiate the exception handler that is responsible for the failure metric.
        globalExceptionHandler = new GlobalExceptionHandler(failedLoginsCounter);
    }

    @Test
    @DisplayName("Given a successful login, should increment the 'success' counter")
    void login_whenSuccessful_shouldIncrementSuccessCounter() {
        // Arrange
        var loginRequest = new LoginRequest("user", "password");
        when(jwtAuthenticationProvider.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(successfulAuthentication);
        when(jwtTokenService.generateToken(any(Authentication.class))).thenReturn("dummy.jwt.token");

        // Act
        authController.login(loginRequest);

        // Assert
        assertEquals(1.0, successfulLoginsCounter.count(), "Success counter should be incremented by 1");
        assertEquals(0.0, failedLoginsCounter.count(), "Failure counter should not be incremented");
    }

    @Test
    @DisplayName("Given a failed login, should increment the 'failure' counter")
    void login_whenAuthenticationFails_shouldIncrementFailureCounter() {
        // Arrange
        var loginRequest = new LoginRequest("user", "wrong-password");
        var exception = new BadCredentialsException("Invalid credentials");
        when(jwtAuthenticationProvider.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(exception);

        // Define the behavior of our mock request
        when(mockRequest.getRequestURI()).thenReturn("/auth/login");

        // Act & Assert for exception
        // We verify that the controller correctly re-throws the exception.
        assertThrows(BadCredentialsException.class, () -> {
            authController.login(loginRequest);
        });

        // Now, we simulate the @RestControllerAdvice behavior by calling the handler directly.
        globalExceptionHandler.handleAuthenticationException(exception, mockRequest);

        // Assert for metrics
        assertEquals(1.0, failedLoginsCounter.count());
        assertEquals(0.0, successfulLoginsCounter.count());
    }
}