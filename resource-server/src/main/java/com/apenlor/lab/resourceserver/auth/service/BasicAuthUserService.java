package com.apenlor.lab.resourceserver.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * An implementation of UserDetailsService for the application's management users (Basic Auth flow).
 * <p>
 * This service is responsible for loading user-specific data for internal, machine-to-machine
 * clients, such as the Prometheus scraper. By externalizing the credentials to application.properties,
 * we adhere to best practices for configuration management, allowing operators to manage
 * these credentials without code changes.
 */
@Service
public class BasicAuthUserService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final String managementUsername;
    private final String managementPassword;
    private final String managementUserRoles;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param passwordEncoder     The shared password encoder bean.
     * @param managementUsername  The username for the management user, injected from properties.
     * @param managementPassword  The password for the management user, injected from properties.
     * @param managementUserRoles The roles for the management user, injected from properties.
     */
    public BasicAuthUserService(
            PasswordEncoder passwordEncoder,
            @Value("${ACTUATOR_USERNAME}") String managementUsername,
            @Value("${ACTUATOR_PASSWORD}") String managementPassword,
            @Value("${ACTUATOR_ROLES}") String managementUserRoles) {
        this.passwordEncoder = passwordEncoder;
        this.managementUsername = managementUsername;
        this.managementPassword = managementPassword;
        this.managementUserRoles = managementUserRoles;
    }

    /**
     * Loads the details for the management user.
     * <p>
     * Architectural Decision:
     * This method returns a NEW UserDetails object for each invocation. This is a
     * critical security practice to prevent credential erasure by the Spring Security
     * framework after a successful authentication.
     *
     * @param username The username to look up (expected to match the configured management username).
     * @return A fresh, fully-populated UserDetails object for the management user.
     * @throws UsernameNotFoundException If the provided username does not match the configured one.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (managementUsername.equals(username)) {
            return User.withUsername(managementUsername)
                    .password(passwordEncoder.encode(managementPassword))
                    .roles(managementUserRoles)
                    .build();
        }
        throw new UsernameNotFoundException("Management user not found: " + username);
    }
}