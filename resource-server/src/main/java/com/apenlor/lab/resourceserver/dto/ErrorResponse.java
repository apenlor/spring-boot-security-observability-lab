package com.apenlor.lab.resourceserver.dto;

import java.time.Instant;

/**
 * A standardized DTO for returning API error responses.
 *
 * @param timestamp The time the error occurred.
 * @param status The HTTP status code.
 * @param error The high-level error description.
 * @param message The detailed error message.
 * @param path The path where the error occurred.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}