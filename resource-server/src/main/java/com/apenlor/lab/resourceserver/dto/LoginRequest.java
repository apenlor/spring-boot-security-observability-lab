package com.apenlor.lab.resourceserver.dto;

/**
 * Represents the data structure for a login request.
 *
 * @param username The user's username.
 * @param password The user's password.
 */
public record LoginRequest(String username, String password) {
}
