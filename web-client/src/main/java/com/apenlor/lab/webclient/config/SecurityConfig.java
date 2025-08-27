package com.apenlor.lab.webclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The primary web security configuration for the OIDC Web Client application.
 * <p>
 * This class configures the Spring Security filter chain to handle user authentication
 * via the OIDC Authorization Code Grant flow and to manage a complete, federated logout.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    /**
     * Constructs the security configuration with its required dependency.
     *
     * @param clientRegistrationRepository The repository of configured OIDC clients.
     */
    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Defines the main security filter chain for the application.
     *
     * @param http The HttpSecurity object to be configured.
     * @return A configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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

        //We use '{baseUrl}' as a placeholder that Spring will automatically replace with the
        //redirect url configured in Keycloak for our application.
        successHandler.setPostLogoutRedirectUri("{baseUrl}");

        return successHandler;
    }
}