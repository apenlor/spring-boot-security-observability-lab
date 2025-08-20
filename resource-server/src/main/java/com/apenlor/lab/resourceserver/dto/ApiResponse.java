package com.apenlor.lab.resourceserver.dto;

/**
 * A standardized DTO for returning simple API message responses.
 *
 * @param message The content of the response message.
 */
public record ApiResponse(String message) {
}