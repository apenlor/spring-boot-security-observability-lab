package com.apenlor.lab.resourceserver.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * A component responsible for all JWT (JSON Web Token) operations.
 * <p>
 * This class encapsulates the logic for creating, parsing, and validating JWTs,
 * acting as the single source of truth for token handling. This ensures that the rest of
 * the application remains decoupled from the low-level details of the JWT library.
 */
@Component
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final String SCOPE_CLAIM = "scope";
    private static final long TOKEN_VALIDITY_IN_HOURS = 1L;

    private final SecretKey key;
    private final JwtParser jwtParser;

    /**
     * Constructs the service and initializes the cryptographic key and reusable parser.
     * <p>
     * We use constructor injection for the secret key. This is a best practice as it
     * makes the component's dependencies explicit and ensures the service is immutable and
     * fully configured upon creation, enhancing reliability and testability.
     *
     * @param jwtSecretKey The HS256 secret key, injected from application properties.
     */
    public JwtTokenService(@Value("${jwt.secret.key}") String jwtSecretKey) {
        this.key = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
        this.jwtParser = Jwts.parser().verifyWith(this.key).build();
    }

    /**
     * Generates a JWT for a given successfully authenticated user.
     *
     * @param authentication The successful authentication object containing the user's principal and authorities.
     * @return A signed, compact JWT string.
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        return Jwts.builder()
                .subject(authentication.getName())
                .claim(SCOPE_CLAIM, scope)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_VALIDITY_IN_HOURS, ChronoUnit.HOURS)))
                .signWith(key) // jjwt infers the algorithm from the key type (HS256 for SecretKey)
                .compact();
    }

    /**
     * Extracts the username (the subject claim) from a given JWT.
     *
     * @param token The JWT string to parse.
     * @return The username contained within the token.
     * @throws JwtException if the token cannot be parsed (e.g., invalid signature).
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates a JWT by checking its signature, expiration, and subject claim.
     * This method provides a simple boolean response, suitable for security filters.
     *
     * @param token    The JWT string to validate.
     * @param username The username that the token is expected to belong to.
     * @return {@code true} if the token is valid for the given user, {@code false} otherwise.
     */
    public boolean isTokenValid(String token, String username) {
        try {
            // The parseClaims method verifies the signature. It will throw an exception on failure.
            final Claims claims = parseClaims(token);

            // Explicitly check if the token is expired.
            if (claims.getExpiration().toInstant().isBefore(Instant.now())) {
                log.warn("Validation failed for user '{}': JWT has expired.", username);
                return false;
            }

            // Explicitly check if the token subject matches the expected username.
            if (!username.equals(claims.getSubject())) {
                log.warn("Validation failed: JWT subject '{}' does not match expected username '{}'.", claims.getSubject(), username);
                return false;
            }

            return true;
        } catch (SignatureException e) {
            log.warn("Validation failed: JWT signature is invalid. {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Validation failed: JWT is malformed. {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT validation failed for an unknown reason: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Parses a JWT string and returns its claims payload.
     * The primary purpose of this method is to cryptographically verify the token's signature.
     *
     * @param token The compact JWT string.
     * @return The claims payload of the token.
     * @throws JwtException if the token signature is invalid or the token is malformed.
     */
    private Claims parseClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }
}