package com.apenlor.lab.webclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for the WebClient used to make service-to-service calls.
 */
@Configuration
public class WebClientConfig {

    @Value("${oauth2.client.registration.id:lab-client}")
    private String registrationId;

    /**
     * Creates a WebClient bean pre-configured with the OAuth2 filter function.
     * <p>
     * This is the standard, modern way to create an OAuth2-aware WebClient in a
     * servlet-based Spring Boot application. The ServletOAuth2AuthorizedClientExchangeFilterFunction
     * automatically handles the "client credentials" grant flow. When this WebClient is used
     * to call a resource server, this filter will:
     * 1. Check if a valid access token for the client already exists.
     * 2. If not, it will automatically go to the configured token provider (Keycloak)
     * and request a new token using the client's credentials.
     * 3. It will then attach this token as a Bearer token in the Authorization header.
     * This entire process is transparent to the application code that uses the WebClient.
     *
     * @param clientRegistrationRepository Repo for client configurations (from properties).
     * @param authorizedClientRepository   Repo for storing/retrieving authorized clients.
     * @return A configured WebClient instance.
     */
    @Bean
    public WebClient webClient(ClientRegistrationRepository clientRegistrationRepository,
                               OAuth2AuthorizedClientRepository authorizedClientRepository) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrationRepository, authorizedClientRepository);

        oauth2.setDefaultClientRegistrationId(registrationId);

        return WebClient.builder()
                .apply(oauth2.oauth2Configuration())
                .build();
    }
}