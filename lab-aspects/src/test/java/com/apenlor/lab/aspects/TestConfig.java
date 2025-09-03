package com.apenlor.lab.aspects;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Provides necessary beans for the test context.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides a simple, in-memory MeterRegistry for the tests.
     * This allows us to inject a MeterRegistry into our AuditLogAspect
     * and to later make assertions against the metrics it has recorded,
     * without needing a full Prometheus backend.
     *
     * @return A SimpleMeterRegistry instance.
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}