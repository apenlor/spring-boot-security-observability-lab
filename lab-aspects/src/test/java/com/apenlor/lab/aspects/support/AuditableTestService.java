package com.apenlor.lab.aspects.support;

import com.apenlor.lab.aspects.audit.Auditable;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * A simple service with methods annotated with @Auditable.
 * The AOP framework will wrap these methods with our AuditLogAspect,
 * allowing us to trigger and test the aspect's behavior.
 */
@Service
public class AuditableTestService {

    /**
     * Simulates a successful, auditable operation.
     *
     * @return A success message.
     */
    @Auditable
    public String successfulMethod() {
        return "Success";
    }

    /**
     * Simulates a failing, auditable operation.
     *
     * @throws IllegalStateException always.
     */
    @Auditable
    public void failingMethod() {
        throw new IllegalStateException("Simulated business logic failure");
    }

    /**
     * Simulates a failing operation with a nested root cause.
     * This is specifically for testing the root cause logging in the aspect.
     *
     * @throws RuntimeException always.
     */
    @Auditable
    public void failingMethodWithRootCause() {
        try {
            throw new IOException("This is the real root cause");
        } catch (IOException e) {
            throw new RuntimeException("This is a wrapping exception", e);
        }
    }
}