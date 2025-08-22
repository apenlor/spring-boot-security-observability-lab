package com.apenlor.lab.resourceserver.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the BasicAuthUserService.
 * <p>
 * This class verifies the user lookup logic for the application's management users.
 * It ensures that the service correctly uses the injected configuration properties to
 * build the UserDetails for the actuator.
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("BasicAuthUserService Unit Tests")
class BasicAuthUserServiceTest {

    @Value("${test.actuator.username}")
    private String testUsername;
    @Value("${test.actuator.password}")
    private String testPassword;
    @Value("${test.actuator.roles}")
    private String testRoles;

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    private BasicAuthUserService basicAuthUserService;

    @BeforeEach
    void setUp() {
        // Instantiate the service with the mock encoder and hardcoded test properties.
        // This isolates the test from the actual application.properties file.
        basicAuthUserService = new BasicAuthUserService(
                mockPasswordEncoder,
                testUsername,
                testPassword,
                testRoles
        );
    }

    @Nested
    @DisplayName("loadUserByUsername Method")
    class LoadUserByUsername {

        @Test
        @DisplayName("Given the correct actuator username, should return correct UserDetails")
        void withValidUsername_shouldReturnUserDetails() {
            // Arrange
            when(mockPasswordEncoder.encode(testPassword)).thenReturn("{bcrypt}encoded_actuator_password");

            // Act
            UserDetails userDetails = basicAuthUserService.loadUserByUsername(testUsername);

            // Assert
            assertNotNull(userDetails);
            assertEquals(testUsername, userDetails.getUsername());
            assertEquals("{bcrypt}encoded_actuator_password", userDetails.getPassword());

            Set<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            assertEquals(1, authorities.size(), "Should have exactly 1 authority");
            assertTrue(authorities.contains("ROLE_ACTUATOR_ADMIN"), "Authority should be 'ROLE_ACTUATOR_ADMIN'");
        }

        @Test
        @DisplayName("Given any other username, should throw UsernameNotFoundException")
        void withInvalidUsername_shouldThrowUsernameNotFoundException() {
            // Arrange
            String invalidUsername = "not_the_actuator";
            // No mock setup is needed, as the encoder is not called if the username doesn't match.

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> {
                basicAuthUserService.loadUserByUsername(invalidUsername);
            });
        }

        @Test
        @DisplayName("Should return a new UserDetails instance on each call")
        void shouldReturnNewInstanceOnEachCall() {
            // Arrange
            when(mockPasswordEncoder.encode(testPassword)).thenReturn("{bcrypt}encoded_actuator_password");

            // Act
            UserDetails userDetails1 = basicAuthUserService.loadUserByUsername(testUsername);
            UserDetails userDetails2 = basicAuthUserService.loadUserByUsername(testUsername);

            // Assert
            assertNotSame(userDetails1, userDetails2, "Should return a new object instance on every call");
        }
    }
}