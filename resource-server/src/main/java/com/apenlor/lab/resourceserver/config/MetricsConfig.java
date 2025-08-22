package com.apenlor.lab.resourceserver.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A centralized configuration for defining and providing custom application metrics.
 * <p>
 * Architectural Decision:
 * By defining all custom metrics as Spring beans in a single location, we create a
 * "single source of truth." This enforces consistency in metric naming and tagging
 * across the entire application and eliminates the risk of typos in scattered
 * metric definitions. Components can then inject the specific metric beans they need
 * using @Qualifier.
 */
@Configuration
public class MetricsConfig {

    public static final String LOGIN_TOTAL = "auth.logins.total";
    public static final String SECURE_REQUESTS_TOTAL = "api.requests.secure.total";

    @Bean
    public Counter successfulLoginsCounter(MeterRegistry meterRegistry) {
        return Counter.builder(LOGIN_TOTAL)
                .description("Total number of successful logins.")
                .tag("result", "success")
                .register(meterRegistry);
    }

    @Bean
    public Counter failedLoginsCounter(MeterRegistry meterRegistry) {
        return Counter.builder(LOGIN_TOTAL)
                .description("Total number of failed logins.")
                .tag("result", "failure")
                .register(meterRegistry);
    }

    @Bean
    public Counter secureEndpointRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder(SECURE_REQUESTS_TOTAL)
                .description("Total number of requests to the secure data endpoint.")
                .tag("endpoint", "/api/secure/data")
                .register(meterRegistry);
    }
}