package com.apenlor.lab.resourceserver.exception;

import com.apenlor.lab.resourceserver.dto.ErrorResponse;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
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
@Slf4j
public class GlobalExceptionHandler {

    private final Counter failedLoginsCounter;

    public GlobalExceptionHandler(@Qualifier("failedLoginsCounter") Counter failedLoginsCounter) {
        this.failedLoginsCounter = failedLoginsCounter;
    }

    /**
     * Handles authentication-related exceptions thrown from within controllers.
     *
     * @param ex      The caught AuthenticationException.
     * @param request The current HTTP request.
     * @return A ResponseEntity with a 401 status and standardized error body.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        failedLoginsCounter.increment();
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication failed: Invalid credentials provided.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles authorization-related exceptions.
     * These occur when an authenticated user attempts to access a resource they
     * do not have the necessary permissions for.
     *
     * @param ex      The caught AuthenticationException.
     * @param request The current HTTP request.
     * @return A ResponseEntity with a 403 Forbidden status and standardized error body.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex, HttpServletRequest request) {
        log.warn("Authorization failure for request {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Access denied. You do not have the required permissions to access this resource.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * A catch-all handler for any other unhandled runtime exceptions.
     * <p>
     * This handler acts as a safety net. It ensures that any unexpected error in the
     * application does not result in a raw stack trace being sent to the client. Instead,
     * it logs the full error for debugging and returns a generic, standardized 500 error
     * response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}