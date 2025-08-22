package com.apenlor.lab.resourceserver.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the JwtUserService.
 * <p>
 * This class verifies the user lookup logic for the application's primary users.
 * It ensures that the service correctly returns user details for valid users and
 * throws the appropriate exception for unknown users.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUserService Unit Tests")
class JwtUserServiceTest {

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    private JwtUserService jwtUserService;

    @BeforeEach
    void setUp() {
        // Instantiate the service with our mock password encoder.
        jwtUserService = new JwtUserService(mockPasswordEncoder);
    }

    @Nested
    @DisplayName("loadUserByUsername Method")
    class LoadUserByUsername {

        @Test
        @DisplayName("Given a valid username ('user'), should return correct UserDetails")
        void withValidUsername_shouldReturnUserDetails() {
            // Arrange.
            when(mockPasswordEncoder.encode("password")).thenReturn("{bcrypt}encoded_password");

            // Act
            UserDetails userDetails = jwtUserService.loadUserByUsername("user");

            // Assert
            assertNotNull(userDetails);
            assertEquals("user", userDetails.getUsername());
            assertEquals("{bcrypt}encoded_password", userDetails.getPassword());

            // Assert authorities with more precision.
            Set<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            assertEquals(3, authorities.size(), "Should have 3 authorities in total");
            assertTrue(authorities.contains("read"), "Should have 'read' authority");
            assertTrue(authorities.contains("write"), "Should have 'write' authority");
            assertTrue(authorities.contains("ROLE_USER"), "Should have 'ROLE_USER' authority from the .roles() call");
        }

        @Test
        @DisplayName("Given an invalid username, should throw UsernameNotFoundException")
        void withInvalidUsername_shouldThrowUsernameNotFoundException() {
            // Arrange
            String invalidUsername = "unknown_user";
            // No mock setup is needed here, which resolves the UnnecessaryStubbingException.

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> {
                jwtUserService.loadUserByUsername(invalidUsername);
            });
        }

        @Test
        @DisplayName("Should return a new UserDetails instance on each call")
        void shouldReturnNewInstanceOnEachCall() {
            // Arrange
            when(mockPasswordEncoder.encode("password")).thenReturn("{bcrypt}encoded_password");

            // Act
            UserDetails userDetails1 = jwtUserService.loadUserByUsername("user");
            UserDetails userDetails2 = jwtUserService.loadUserByUsername("user");

            // Assert
            assertNotSame(userDetails1, userDetails2, "Should return a new object instance on every call");
        }
    }
}