package com.apenlor.lab.resourceserver.config;

import com.apenlor.lab.resourceserver.service.TokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom JWT authentication filter that runs once per request.
 * This filter is responsible for validating the JWT from the Authorization header
 * and setting the user's authentication in the Spring Security context.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(TokenService tokenService, UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String username = tokenService.getUsernameFromToken(jwt);

            // If a username is extracted and the user is not already authenticated in the current context
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load the user details from the database in real world (or in-memory store for our lab)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Validate the token against the user details
                if (tokenService.isTokenValid(jwt, userDetails.getUsername())) {
                    // Create an authentication token for the user
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // Add details from the current request to the authentication token
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Authenticated user '{}', setting security context", username);
                }
            }
        } catch (JwtException e) {
            log.warn("JWT token processing failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}