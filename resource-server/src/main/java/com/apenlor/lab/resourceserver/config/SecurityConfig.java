package com.apenlor.lab.resourceserver.config;

import com.apenlor.lab.resourceserver.auth.KeycloakRoleConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The primary web security configuration class for the application.
 * <p>
 * This class defines the security filter chains that protect the application's
 * various HTTP endpoints. It leverages Spring Boot's autoconfiguration for
 * Basic Auth (actuator endpoints) and the standard OAuth2 Resource Server DSL for
 * JWT validation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_API_PATHS = {
            "/api/public/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Value("${actuator.roles}")
    private String actuatorAdminRole;

    /**
     * Configures the security filter chain for the management endpoints (Actuator).
     * <p>
     * This chain has the highest precedence (@Order(1)) to isolate actuator security.
     * It uses HTTP Basic Authentication, configured by Spring Boot.
     *
     * @param http The HttpSecurity object to be configured.
     * @return The configured SecurityFilterChain for the management endpoints.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(authorize -> authorize
                        // Allow anonymous access to health and info endpoints.
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().hasRole(actuatorAdminRole)
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * Configures the security filter chain for the main application API.
     * <p>
     * This chain has a lower precedence (@Order(2)) and applies to all other requests.
     * It configures the application as a stateless OAuth2 Resource Server.
     * The '.oauth2ResourceServer()' DSL enables JWT validation based on the
     * "issuer-uri" configured in application.properties.
     *
     * @param http The HttpSecurity object to be configured.
     * @return The configured SecurityFilterChain for the application API.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                // Configure the application as an OAuth2 Resource Server & use custom JWT converter
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return jwtAuthenticationConverter;
    }

}