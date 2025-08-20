package com.apenlor.lab.resourceserver.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the JwtTokenService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenService Unit Tests")
class JwtTokenServiceTest {

    private static final String TEST_SECRET_KEY = "our-super-secret-and-long-key-for-hs256-which-is-at-least-32-bytes-long";
    private static final String TEST_USERNAME = "test-user";

    @Mock
    private Authentication mockAuthentication;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(TEST_SECRET_KEY);
    }

    /**
     * Helper method to configure the mock Authentication object for tests that require it.
     */
    private void setupMockAuthentication() {
        when(mockAuthentication.getName()).thenReturn(TEST_USERNAME);
        when(mockAuthentication.getAuthorities()).thenAnswer((Answer<Collection<GrantedAuthority>>) invocation ->
                List.of(
                        new SimpleGrantedAuthority("read"),
                        new SimpleGrantedAuthority("write")
                )
        );
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        // This setup runs before each test within this nested class.
        @BeforeEach
        void setUp() {
            setupMockAuthentication();
        }

        @Test
        @DisplayName("Should generate a valid, non-empty JWT string")
        void generateToken_shouldReturnValidJwt() {
            String token = jwtTokenService.generateToken(mockAuthentication);
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("Generated token should contain the correct subject (username)")
        void generateToken_shouldContainCorrectSubject() {
            String token = jwtTokenService.generateToken(mockAuthentication);
            String usernameFromToken = jwtTokenService.getUsernameFromToken(token);
            assertEquals(TEST_USERNAME, usernameFromToken);
        }

        @Test
        @DisplayName("Generated token should have an expiration date in the future")
        void generateToken_shouldHaveFutureExpiration() {
            String token = jwtTokenService.generateToken(mockAuthentication);
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes()))
                    .build().parseSignedClaims(token);
            Date expiration = claimsJws.getPayload().getExpiration();
            assertNotNull(expiration);
            assertTrue(expiration.after(new Date()));
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("isTokenValid should return true for a valid, unexpired token")
        void isTokenValid_withValidToken_shouldReturnTrue() {
            // Arrange
            setupMockAuthentication();
            String token = jwtTokenService.generateToken(mockAuthentication);

            // Act & Assert
            assertTrue(jwtTokenService.isTokenValid(token, TEST_USERNAME));
        }

        @Test
        @DisplayName("isTokenValid should return false for an expired token")
        void isTokenValid_withExpiredToken_shouldReturnFalse() {
            String expiredToken = Jwts.builder()
                    .subject(TEST_USERNAME)
                    .expiration(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)))
                    .signWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes()))
                    .compact();
            assertFalse(jwtTokenService.isTokenValid(expiredToken, TEST_USERNAME));
        }

        @Test
        @DisplayName("isTokenValid should return false for a token with a mismatched username")
        void isTokenValid_withMismatchedUsername_shouldReturnFalse() {
            // Arrange: Setup the mock, as it's needed to generate the token.
            setupMockAuthentication();
            String token = jwtTokenService.generateToken(mockAuthentication);

            // Act & Assert
            assertFalse(jwtTokenService.isTokenValid(token, "wrong-user"));
        }

        @Test
        @DisplayName("isTokenValid should return false for a token signed with a different key")
        void isTokenValid_withInvalidSignature_shouldReturnFalse() {
            SecretKey wrongKey = Keys.hmacShaKeyFor("another-different-secret-key-that-is-also-long-enough".getBytes());
            String tokenWithWrongSignature = Jwts.builder()
                    .subject(TEST_USERNAME)
                    .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                    .signWith(wrongKey)
                    .compact();
            assertFalse(jwtTokenService.isTokenValid(tokenWithWrongSignature, TEST_USERNAME));
        }

        @Test
        @DisplayName("getUsernameFromToken should throw JwtException for a malformed token")
        void getUsernameFromToken_withMalformedToken_shouldThrowException() {
            String malformedToken = "this.is.not.a.jwt";
            assertThrows(JwtException.class, () -> jwtTokenService.getUsernameFromToken(malformedToken));
        }
    }
}