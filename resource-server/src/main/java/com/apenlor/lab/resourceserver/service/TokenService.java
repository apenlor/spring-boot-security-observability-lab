package com.apenlor.lab.resourceserver.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Service responsible for JWT (JSON Web Token) generation and validation.
 * This class encapsulates all logic related to token handling.
 */
@Service
public class TokenService {

    @Value("${jwt.secret.key}")
    private String jwtSecretKey;

    private SecretKey key;

    /**
     * Initializes the service by creating a secure SecretKey from the configured string.
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
    }

    /**
     * Generates a JWT for a given authenticated user.
     *
     * @param authentication The successful authentication object from the AuthenticationManager.
     * @return A signed JWT string.
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();

        // The 'scope' claim will contain the user's authorities (e.g., "read write").
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        return Jwts.builder()
                .subject(authentication.getName())
                // Add the dummy 'scope' claim.
                .claim("scope", scope)
                .issuedAt(Date.from(now))
                // Set the token expiration time (1 hour from now).
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                // Sign the token with the secret key using the HS256 algorithm.
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }


    /**
     * Extracts the username (subject) from a given JWT.
     *
     * @param token The JWT string.
     * @return The username.
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Validates a JWT. It checks for a valid signature and ensures the token is not expired.
     *
     * @param token The JWT string.
     * @return true if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    private Claims parseToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    private boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}