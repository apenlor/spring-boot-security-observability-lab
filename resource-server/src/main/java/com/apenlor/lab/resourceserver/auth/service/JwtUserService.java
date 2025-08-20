package com.apenlor.lab.resourceserver.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * An implementation of UserDetailsService for the application's primary users (JWT flow).
 * <p>
 * This service is responsible for "fake-loading" user-specific data from a data store.
 * For the purposes of this lab, we use a hardcoded application user,
 * acting as a stand-in for a database-backed user repository.
 */
@Service
@RequiredArgsConstructor
public class JwtUserService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    /**
     * Locates the user based on the username.
     * <p>
     * Architectural Decision:
     * This method returns a NEW UserDetails object for each invocation. This is a
     * critical security practice. After a successful password-based authentication,
     * Spring Security, by default, erases the credentials from the returned UserDetails
     * object to prevent sensitive data from lingering in memory. By returning a fresh,
     * immutable copy each time, we ensure that our underlying "user store" is not
     * mutated, preventing authentication failures on subsequent requests.
     *
     * @param username The username identifying the user whose data is required.
     * @return A fully populated UserDetails object (never <code>null</code>).
     * @throws UsernameNotFoundException if the user could not be found.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // In a real-world application, this is where you would query your database.

        if ("user".equals(username)) {
            // We use the injected PasswordEncoder to ensure the password is in the
            // correct, encoded format that the DaoAuthenticationProvider expects.
            return User.withUsername("user")
                    .password(passwordEncoder.encode("password"))
                    .authorities("ROLE_USER", "read", "write")
                    .build();
        }
        throw new UsernameNotFoundException("Application user not found: " + username);
    }
}