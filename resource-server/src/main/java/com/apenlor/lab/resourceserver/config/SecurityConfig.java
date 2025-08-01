package com.apenlor.lab.resourceserver.config;

import com.apenlor.lab.resourceserver.service.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(TokenService tokenService, UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Defines the main security filter chain for the application. This bean is the primary
     * point of configuration for all security aspects of the web layer.
     *
     * @param http The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF protection, as it's not needed for stateless, token-based APIs.
                .csrf(AbstractHttpConfigurer::disable)
                // Configure authorization rules for all incoming HTTP requests.
                .authorizeHttpRequests(auth -> auth
                        // Allow unauthenticated access to the public info and login endpoints.
                        .requestMatchers("/api/public/info", "/auth/login").permitAll()
                        // All other requests must be authenticated.
                        .anyRequest().authenticated())
                // Add our custom JWT filter before the standard UsernamePasswordAuthenticationFilter.
                .addFilterBefore(new JwtAuthenticationFilter(tokenService, userDetailsService), UsernamePasswordAuthenticationFilter.class)
                // Configure auth exceptions to return 401 Unauthorized response.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // Configure session management to be stateless.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build();
    }

}