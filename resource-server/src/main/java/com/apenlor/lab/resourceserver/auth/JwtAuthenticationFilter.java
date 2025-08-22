package com.apenlor.lab.resourceserver.auth;

import com.apenlor.lab.resourceserver.auth.service.JwtTokenService;
import com.apenlor.lab.resourceserver.auth.service.JwtUserService;
import io.jsonwebtoken.JwtException;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * A custom Spring Security filter that intercepts all incoming HTTP requests to
 * validate JWTs present in the 'Authorization' header.
 * <p>
 * This filter is the cornerstone of the application's stateless, token-based
 * authentication. It runs once per request and is responsible for parsing the
 * Bearer token, validating its signature and claims, and populating the
 * SecurityContextHolder with the user's authentication details if the token is valid.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final JwtUserService jwtUserService;
    private final Counter failedLoginsCounter;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, JwtUserService jwtUserService,
                                   @Qualifier("failedLoginsCounter") Counter failedLoginsCounter) {
        this.jwtTokenService = jwtTokenService;
        this.jwtUserService = jwtUserService;
        this.failedLoginsCounter = failedLoginsCounter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Attempt to extract the JWT from the request header.
        extractTokenFromRequest(request).ifPresent(jwt -> {
            try {
                // If a token is found, attempt to process it.
                processToken(request, jwt);
            } catch (JwtException e) {
                // Log JWT-specific exceptions but do not break the chain.
                log.warn("JWT processing failed: {}. Path: {}", e.getMessage(), request.getRequestURI());
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                failedLoginsCounter.increment();
            }
        });

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT Bearer token from the 'Authorization' header of the request.
     *
     * @param request The incoming HTTP request.
     * @return An Optional containing the JWT string if present and correctly formatted, otherwise an empty Optional.
     */
    private Optional<String> extractTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }
        return Optional.empty();
    }

    /**
     * Processes a valid JWT by extracting the username, loading user details,
     * and setting the authentication in the Spring Security context.
     *
     * @param request The current HTTP request, used to build authentication details.
     * @param jwt     The JWT string to process.
     */
    private void processToken(HttpServletRequest request, String jwt) {
        final String username = jwtTokenService.getUsernameFromToken(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.jwtUserService.loadUserByUsername(username);

            if (jwtTokenService.isTokenValid(jwt, userDetails.getUsername())) {
                // If the token is valid, create a new authentication token.
                // This token represents the authenticated user for the duration of the request.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Credentials are null for token-based auth
                        userDetails.getAuthorities()
                );
                // Enhance the authentication token with details from the web request.
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user '{}', setting security context", username);
            }
        }
    }
}