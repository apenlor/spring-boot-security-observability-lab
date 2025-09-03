package com.apenlor.lab.aspects;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A minimal Spring Boot application entry point for the test context.
 * This class enables component scanning within this test package, allowing
 * our TestConfig, AuditableTestService, and the AuditLogAspect itself to be
 * discovered and wired together by the Spring container during tests.
 */
@SpringBootApplication
public class TestApplication {
}