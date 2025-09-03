package com.apenlor.lab.aspects.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import com.apenlor.lab.aspects.support.AuditableTestService;
import com.apenlor.lab.aspects.TestApplication;
import com.apenlor.lab.aspects.TestConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import net.logstash.logback.encoder.LogstashEncoder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {TestApplication.class, TestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuditLogAspectTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private AuditableTestService auditableTestService;
    @Autowired
    private MeterRegistry meterRegistry;
    @MockitoBean
    private MockHttpServletRequest mockRequest;
    private ByteArrayOutputStream outputStream;
    private Appender<ILoggingEvent> appender;
    private Logger auditLogger;

    @BeforeEach
    void setUp() {
        MDC.clear();
        auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        outputStream = new ByteArrayOutputStream();

        OutputStreamAppender<ILoggingEvent> newAppender = new OutputStreamAppender<>();
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.start(); // Encoder must be started
        newAppender.setEncoder(encoder);
        newAppender.setOutputStream(outputStream);
        newAppender.start(); // Appender must be started

        this.appender = newAppender;
        auditLogger.addAppender(this.appender);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        auditLogger.detachAppender(this.appender);
        appender.stop();
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("Should log SUCCESS audit event for successful method execution")
    @WithMockUser(username = "testuser", roles = {"USER", "READER"})
    void testSuccessfulMethodExecution() throws Exception {
        // Given
        String longUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " + "a".repeat(300);
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.123");
        when(mockRequest.getHeader("User-Agent")).thenReturn(longUserAgent);

        // When
        auditableTestService.successfulMethod();

        // Then
        Map<String, Object> auditDetails = getAuditDetailsFromLog();

        assertThat(auditDetails)
                .containsEntry("outcome", "SUCCESS")
                .containsEntry("principal", "testuser")
                .containsEntry("remoteAddr", "192.168.1.XXX")
                .hasEntrySatisfying("durationMs", value -> assertThat((Double) value).isGreaterThanOrEqualTo(0))
                .hasEntrySatisfying("userAgent", value -> {
                    assertThat((String) value).hasSizeLessThanOrEqualTo(256);
                    assertThat((String) value).endsWith("...");
                });

        assertThat(auditDetails.get("roles"))
                .isInstanceOf(List.class)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_READER");

        Counter successCounter = meterRegistry.find("app.audit.events.total")
                .tags("method", "AuditableTestService.successfulMethod()", "outcome", "SUCCESS")
                .counter();
        assertThat(successCounter).isNotNull();
        assertThat(successCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should log FAILURE audit event when method throws exception")
    @WithMockUser(username = "erroruser", roles = {"ADMIN"})
    void testFailingMethodExecution() throws Exception {
        // When & Then
        assertThrows(IllegalStateException.class, () -> auditableTestService.failingMethod());

        Map<String, Object> auditDetails = getAuditDetailsFromLog();

        assertThat(auditDetails)
                .containsEntry("outcome", "FAILURE")
                .containsEntry("exceptionType", "IllegalStateException");

        Counter failureCounter = meterRegistry.find("app.audit.events.total")
                .tags("method", "AuditableTestService.failingMethod()", "outcome", "FAILURE")
                .counter();
        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should log failure event with root cause information")
    @WithMockUser(username = "rootcauseuser")
    void testFailingMethodWithRootCause() throws Exception {
        // When & Then
        assertThrows(RuntimeException.class, () -> auditableTestService.failingMethodWithRootCause());

        // Then
        Map<String, Object> auditDetails = getAuditDetailsFromLog();

        assertThat(auditDetails)
                .containsEntry("outcome", "FAILURE")
                .containsEntry("principal", "rootcauseuser")
                .containsEntry("exceptionType", "RuntimeException")
                .containsEntry("rootCauseType", "IOException")
                .containsEntry("rootCauseMessage", "This is the real root cause");

        Counter failureCounter = meterRegistry.find("app.audit.events.total")
                .tags("method", "AuditableTestService.failingMethodWithRootCause()", "outcome", "FAILURE")
                .counter();
        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle NON_WEB context gracefully")
    void testNonWebContextExecution() throws Exception {
        // Given a non-web context where the request is null
        RequestContextHolder.resetRequestAttributes();

        // When
        auditableTestService.successfulMethod();

        // Then
        Map<String, Object> auditDetails = getAuditDetailsFromLog();

        // Assert that the context is correctly identified and web-specific fields are absent.
        assertThat(auditDetails)
                .containsEntry("contextType", "NON_WEB")
                .containsEntry("principal", "anonymous") // No security context in this test
                .doesNotContainKey("remoteAddr")
                .doesNotContainKey("requestUri")
                .doesNotContainKey("userAgent");
    }

    private Map<String, Object> getAuditDetailsFromLog() throws Exception {
        // Reads the final, serialized JSON string
        String fullLogJson = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(fullLogJson).isNotBlank();

        // We parse this string and extract the nested "audit" object.
        JsonNode rootNode = objectMapper.readTree(fullLogJson);
        JsonNode auditNode = rootNode.path("audit");
        assertThat(auditNode.isObject()).withFailMessage("Log JSON should contain a nested 'audit' object").isTrue();

        return objectMapper.convertValue(auditNode, new TypeReference<>() {
        });
    }

}