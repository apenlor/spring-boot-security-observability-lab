package com.apenlor.lab.resourceserver.exception;

import com.apenlor.lab.resourceserver.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * A centralized exception handler for the entire application.
 * This class intercepts exceptions thrown from any @RestController and translates
 * them into a standardized, client-friendly JSON format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles authentication-related exceptions thrown from within controllers.
     *
     * @param ex      The caught AuthenticationException.
     * @param request The current HTTP request.
     * @return A ResponseEntity with a 401 status and standardized error body.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication failed: Invalid credentials provided.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
}