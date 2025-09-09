package com.apenlor.lab.webclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * The primary web security configuration for the OIDC Web Client application.
 * <p>
 * Configures two separate security filter chains: one for the stateful, user-facing
 * OIDC application, and another for the stateless, machine-to-machine actuator endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${actuator.roles}")
    private String actuatorAdminRole;

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Configures the security filter chain for the management endpoints (Actuator).
     * This chain has the highest precedence to isolate it from the main app's security.
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
                        // Allow anonymous access to non-sensitive health and info endpoints.
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Secure all other actuator endpoints.
                        .anyRequest().hasRole(actuatorAdminRole)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                // Prevent the main app's OIDC login page from handling actuator auth failures.
                // This forces a clean 401 Unauthorized, which is correct for machine-to-machine clients.
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .build();
    }

    /**
     * Defines the main security filter chain for the user-facing application.
     *
     * @param http The HttpSecurity object to be configured.
     * @return A configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Allow anonymous access to the root /home page.
                        .requestMatchers("/").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        // On successful login, always redirect the user to the dashboard.
                        .defaultSuccessUrl("/dashboard", true)
                )
                // Configure the Logout feature to notify Keycloak.
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler()));
        return http.build();
    }

    /**
     * Creates the OIDC Logout Success Handler.
     * <p>
     * By default, Spring Security's logout only terminates the local application session.
     * The OidcClientInitiatedLogoutSuccessHandler implements the OIDC RP-Initiated Logout
     * specification. It knows how to construct the correct redirect URL to Keycloak's
     * end-session endpoint, including the necessary parameters to ensure the user is
     * logged out globally. This is critical for a complete Single Sign-Out experience.
     *
     * @return A configured OIDC logout handler.
     */
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler =
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);

        // We use '{baseUrl}' as a placeholder that Spring will automatically replace with the
        // redirect url configured in Keycloak for our application.
        successHandler.setPostLogoutRedirectUri("{baseUrl}");

        return successHandler;
    }
}