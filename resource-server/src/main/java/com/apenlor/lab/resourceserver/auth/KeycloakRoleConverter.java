package com.apenlor.lab.resourceserver.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A custom Spring Security Converter to extract roles from a Keycloak-issued JWT.
 * This is necessary because Keycloak nests roles within a 'realm_access' JSON object,
 * which the default converters cannot parse.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null || realmAccess.isEmpty()) {
            return List.of(); // No roles present
        }

        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) realmAccess.get("roles");

        return roles.stream()
                .map(roleName -> "ROLE_" + roleName) // Prefix with ROLE_ for Spring Security
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}