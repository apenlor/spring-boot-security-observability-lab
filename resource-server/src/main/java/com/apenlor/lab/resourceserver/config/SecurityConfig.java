package com.apenlor.lab.resourceserver.config;

import com.apenlor.lab.resourceserver.auth.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The primary web security configuration class for the application.
 * <p>
 * This class is responsible for defining and assembling the security filter chains
 * that protect the application's various HTTP endpoints. It follows a modern,
 * component-based approach by defining multiple, ordered SecurityFilterChain beans,
 * allowing for distinct security policies for different parts of the application.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider jwtAuthenticationProvider;
    private final AuthenticationProvider basicAuthenticationProvider;

    /**
     * Constructs the SecurityConfig with its required, specifically qualified dependencies.
     * By using @Qualifier on the constructor parameters, we ensure the correct
     * AuthenticationProvider is injected for each security domain.
     *
     * @param jwtAuthenticationFilter     The custom filter for processing JWTs.
     * @param jwtAuthenticationProvider   The provider for the main application's login flow.
     * @param basicAuthenticationProvider The provider for the actuator's Basic Auth flow.
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Qualifier("jwtAuthenticationProvider") AuthenticationProvider jwtAuthenticationProvider,
            @Qualifier("basicAuthenticationProvider") AuthenticationProvider basicAuthenticationProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.basicAuthenticationProvider = basicAuthenticationProvider;
    }

    /**
     * Configures the security filter chain for the management endpoints (Actuator).
     * <p>
     * Architectural Decision:
     * This chain is given the highest precedence (@Order(1)) to ensure that any request
     * to '/actuator/**' is handled exclusively by this specialized configuration.
     * It enforces HTTP Basic Authentication, a simple and standard mechanism suitable for
     * trusted, internal-network clients like a Prometheus scraper.
     *
     * @param http The HttpSecurity object to be configured.
     * @return The configured SecurityFilterChain for the management endpoints.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().hasRole("ACTUATOR_ADMIN")
                )
                .httpBasic(Customizer.withDefaults())
                // We explicitly wire the dedicated provider for actuator credentials.
                .authenticationProvider(basicAuthenticationProvider)
                // Actuator endpoints are machine-to-machine and do not require sessions or CSRF protection.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * Configures the default, catch-all security filter chain for the main application API.
     * <p>
     * Architectural Decision:
     * This chain has a lower precedence (@Order(2)) and no security matcher, meaning it
     * applies to any request not handled by a higher-order chain. It implements a stateless,
     * token-based security model using a custom JWT filter. Publicly accessible endpoints
     * are explicitly permitted, while all others require a valid JWT.
     *
     * @param http The HttpSecurity object to be configured.
     * @return The configured SecurityFilterChain for the application API.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain applicationFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/public/info", "/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                // Add our custom JWT filter to the chain before the username/password filter.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // We explicitly wire the dedicated provider for application user credentials.
                .authenticationProvider(jwtAuthenticationProvider)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                // Set the entry point for authentication failures to return a 401 Unauthorized.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .build();
    }
}