package com.apenlor.lab.resourceserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class ApplicationConfig {

    /**
     * Creates a PasswordEncoder bean that uses a delegating strategy. This standard
     * allows for multiple encoding algorithms to coexist (e.g., bcrypt, scrypt).
     *
     * @return A PasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Defines the user details service that provides user information to Spring Security.
     * For Phase 1, we use a simple in-memory user.
     *
     * @param passwordEncoder The password encoder to use for encoding the user's password.
     * @return A UserDetailsService instance.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // --- Architectural Note: In-Memory User Store ---
        // For this lab phase, we define a single user in memory. The password is
        // encoded using the application's standard PasswordEncoder. In a production
        // scenario, this would be replaced with a database-backed UserDetailsService.
        var user = User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .authorities("read", "write").build();
        return new InMemoryUserDetailsManager(user);
    }

    /**
     * Defines the AuthenticationManager, which is the core of Spring Security's
     * authentication mechanism. It processes an authentication request.
     *
     * @param userDetailsService The service to load user-specific data.
     * @param passwordEncoder    The encoder to use for password verification.
     * @return An AuthenticationManager instance.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }
}