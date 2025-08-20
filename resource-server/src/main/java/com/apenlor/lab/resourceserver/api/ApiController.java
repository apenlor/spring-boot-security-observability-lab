package com.apenlor.lab.resourceserver.api;

import com.apenlor.lab.resourceserver.dto.ApiResponse;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The main REST controller for the application's business endpoints.
 * <p>
 * This controller provides both public and protected resources to demonstrate the
 * effectiveness of the JWT-based security configuration.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final Counter secureEndpointRequestCounter;

    public ApiController(@Qualifier("secureEndpointRequestCounter") Counter secureEndpointRequestCounter) {
        this.secureEndpointRequestCounter = secureEndpointRequestCounter;
    }

    /**
     * An unsecured endpoint that provides public information.
     * This endpoint is configured in SecurityConfig to be accessible by anyone.
     *
     * @return A standard API response with a public message.
     */
    @GetMapping("/public/info")
    @Timed(value = "http.requests.api", description = "Time taken to return public API info", extraTags = {"endpoint", "public"})
    public ResponseEntity<ApiResponse> getPublicInfo() {
        return ResponseEntity.ok(new ApiResponse("This is PUBLIC information. Anyone can see this."));
    }

    /**
     * A secured endpoint that provides sensitive data.
     * This endpoint is protected and requires a valid JWT for access.
     *
     * @param authentication The authenticated principal for the current request.
     * @return A standard API response containing a personalized secure message.
     */
    @GetMapping("/secure/data")
    @Timed(value = "http.requests.api", description = "Time taken to return secure API data", extraTags = {"endpoint", "secure"})
    public ResponseEntity<ApiResponse> getSecureData(Authentication authentication) {
        secureEndpointRequestCounter.increment();
        String username = authentication.getName();
        String message = String.format("This is SECURE data for user: %s. You should only see this if you are authenticated.", username);
        return ResponseEntity.ok(new ApiResponse(message));
    }
}