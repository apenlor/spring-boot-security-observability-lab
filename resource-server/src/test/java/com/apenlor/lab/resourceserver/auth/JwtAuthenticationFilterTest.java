package com.apenlor.lab.resourceserver.auth;

import com.apenlor.lab.resourceserver.auth.service.JwtTokenService;
import com.apenlor.lab.resourceserver.auth.service.JwtUserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the JwtAuthenticationFilter.
 * <p>
 * This class tests the filter's core logic in isolation, verifying that it correctly
 * processes the Authorization header and populates the SecurityContextHolder when
 * a valid JWT is present.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private JwtUserService jwtUserService;
    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private HttpServletResponse mockResponse;
    @Mock
    private FilterChain mockFilterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private Counter failedLoginsCounter;

    @BeforeEach
    void setUp() {
        // Clear the security context before each test to ensure isolation.
        SecurityContextHolder.clearContext();

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        this.failedLoginsCounter = Counter.builder("auth.logins.total")
                .tag("result", "failure")
                .register(meterRegistry);

        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService, jwtUserService, failedLoginsCounter);
    }

    @Test
    @DisplayName("Given a valid token, should populate SecurityContextHolder and proceed")
    void doFilterInternal_withValidToken_shouldPopulateSecurityContext() throws Exception {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "test-user";
        UserDetails userDetails = User.withUsername(username).password("pw").roles("USER").build();

        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenService.getUsernameFromToken(validToken)).thenReturn(username);
        when(jwtUserService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenService.isTokenValid(validToken, username)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Security context should be populated");
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName(), "Authenticated principal should be the correct user");
        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
        assertEquals(0.0, failedLoginsCounter.count(), "Failure counter should not be incremented");
    }

    @Test
    @DisplayName("Given an invalid token, should not populate SecurityContext and increment failure counter")
    void doFilterInternal_withInvalidToken_shouldNotPopulateContextAndIncrementCounter() throws Exception {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        String username = "test-user";
        UserDetails userDetails = User.withUsername(username).password("pw").roles("USER").build();

        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtTokenService.getUsernameFromToken(invalidToken)).thenReturn(username);
        when(jwtUserService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenService.isTokenValid(invalidToken, username)).thenReturn(false); // The key difference

        // Act
        jwtAuthenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context should remain empty");
        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
        assertEquals(1.0, failedLoginsCounter.count(), "Failure counter should be incremented");
    }

    @Test
    @DisplayName("Given no token, should not populate SecurityContext and not increment counter")
    void doFilterInternal_withoutToken_shouldNotPopulateContext() throws Exception {
        // Arrange
        when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context should remain empty");
        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
        assertEquals(0.0, failedLoginsCounter.count(), "Failure counter should not be incremented");
    }

    @Test
    @DisplayName("Given an Authorization header without 'Bearer ', should do nothing")
    void doFilterInternal_withWrongHeaderFormat_shouldDoNothing() throws Exception {
        // Arrange
        when(mockRequest.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNzd29yZA=="); // A Basic Auth header

        // Act
        jwtAuthenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context should remain empty");
        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
        assertEquals(0.0, failedLoginsCounter.count(), "Failure counter should not be incremented");
    }
}