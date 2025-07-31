package com.apenlor.lab.resourceserver.dto;

/**
 * Represents the data structure for a successful login response.
 *
 * @param jwtToken The generated JSON Web Token.
 */
public record LoginResponse(String jwtToken) {
}
