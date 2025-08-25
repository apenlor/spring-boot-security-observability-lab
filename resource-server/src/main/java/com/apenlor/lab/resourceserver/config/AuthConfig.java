package com.apenlor.lab.resourceserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Provides foundational, non-web-specific authentication beans for the entire application.
 * Its responsibility is to create the secure in-memory user for the actuator endpoints.
 */
@Configuration
public class AuthConfig {

    @Value("${spring.security.user.name}")
    private String actuatorUsername;

    @Value("${spring.security.user.password}")
    private String actuatorPassword;

    @Value("${spring.security.user.roles}")
    private String actuatorRoles;

    /**
     * Creates the single, shared PasswordEncoder bean for the entire application.
     *
     * @return A centrally configured PasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Creates an in-memory user details manager for the actuator user.
     * <p>
     * This bean explicitly provides a UserDetailsService to the Spring Security context.
     * Not created automatically due to relying on the oauth integration for the business api.
     *
     * @param passwordEncoder The application's primary password encoder.
     * @return A configured InMemoryUserDetailsManager.
     */
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {
        UserDetails actuatorUser = User.withUsername(actuatorUsername)
                .password(passwordEncoder.encode(actuatorPassword))
                .roles(actuatorRoles)
                .build();
        return new InMemoryUserDetailsManager(actuatorUser);
    }
}