package com.apenlor.lab.resourceserver.auth;

import com.apenlor.lab.resourceserver.auth.service.BasicAuthUserService;
import com.apenlor.lab.resourceserver.auth.service.JwtUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides foundational, non-web-specific authentication beans for the entire application.
 * This class acts as a central factory for authentication components.
 */
@Configuration
public class AuthConfig {

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
     * Creates the dedicated AuthenticationProvider for the main application's login flow.
     * This provider is specifically wired to the JwtUserService and is responsible for
     * handling username/password authentication for application users.
     *
     * @param jwtUserService  The user service that provides application user details.
     * @param passwordEncoder The application's primary password encoder.
     * @return A configured DaoAuthenticationProvider for the JWT login flow.
     */
    @Bean
    public AuthenticationProvider jwtAuthenticationProvider(JwtUserService jwtUserService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(jwtUserService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Creates the dedicated AuthenticationProvider for the actuator's Basic Auth flow.
     * This provider is specifically wired to the BasicAuthUserService and handles
     * authentication for management and monitoring clients.
     *
     * @param basicAuthUserService The user service that provides actuator user details.
     * @param passwordEncoder      The application's primary password encoder.
     * @return A configured DaoAuthenticationProvider for the actuator's Basic Auth flow.
     */
    @Bean
    public AuthenticationProvider basicAuthenticationProvider(BasicAuthUserService basicAuthUserService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(basicAuthUserService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}