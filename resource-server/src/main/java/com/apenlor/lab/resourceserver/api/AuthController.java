package com.apenlor.lab.resourceserver.api;

import com.apenlor.lab.resourceserver.auth.service.JwtTokenService;
import com.apenlor.lab.resourceserver.dto.LoginRequest;
import com.apenlor.lab.resourceserver.dto.LoginResponse;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for handling user authentication and issuing JWTs.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationProvider jwtAuthenticationProvider;
    private final JwtTokenService jwtTokenService;

    private final Counter successfulLoginsCounter;

    public AuthController(
            @Qualifier("jwtAuthenticationProvider") AuthenticationProvider jwtAuthenticationProvider,
            JwtTokenService jwtTokenService,
            @Qualifier("successfulLoginsCounter") Counter successfulLoginsCounter) {
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.jwtTokenService = jwtTokenService;
        this.successfulLoginsCounter = successfulLoginsCounter;
    }

    /**
     * Handles the login request from a user.
     * <p>
     * On a successful authentication, it generates and returns a JWT.
     * On failure, it throws an {@link AuthenticationException}, which is handled
     * by the GlobalExceptionHandler.
     *
     * @param loginRequest The request body containing username and password.
     * @return A ResponseEntity containing the JWT if authentication is successful.
     */
    @PostMapping("/login")
    @Timed(value = "http.requests.auth", description = "Time taken to process a login request")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = jwtAuthenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        String token = jwtTokenService.generateToken(authentication);
        successfulLoginsCounter.increment();

        return ResponseEntity.ok(new LoginResponse(token));
    }
}