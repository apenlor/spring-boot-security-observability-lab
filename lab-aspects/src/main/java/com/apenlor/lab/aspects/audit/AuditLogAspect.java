package com.apenlor.lab.aspects.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An AOP Aspect that intercepts methods annotated with @Auditable to create
 * an observability-integrated audit log.
 * <p>
 * Architectural Decisions:
 * - Emits a structured JSON log via SLF4J's Fluent API for detailed analysis.
 * - Enriches audit logs with 'trace_id' and 'span_id' from the MDC for correlation.
 * - Emits Micrometer Counters and Timers for every audit event, enabling dashboards and alerts.
 * - Metrics are tagged with low-cardinality fields ("method", "outcome") to prevent cardinality explosion.
 * - Caches Meter instances to avoid performance overhead from repeated lookups.
 * - Sanitizes sensitive data (IPs, exception messages) and includes root cause for log hygiene and debuggability.
 * - Handles both web and non-web execution contexts gracefully and enriches with user roles.
 * <p>
 * Deferred Improvements (for future consideration):
 * - Deeper PII masking (e.g., request parameters, other headers based on configuration).
 * - Configurable audit verbosity via "@Auditable" attributes (e.g., "eventType", "sensitivityLevel").
 * - Advanced log injection prevention (e.g., OWASP ESAPI encoder for log messages).
 * - Dedicated unit/integration tests for the aspect's various scenarios (mocking MDC/SecurityContext).
 */
@Aspect
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class AuditLogAspect {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");
    private static final Marker AUDIT_MARKER = MarkerFactory.getMarker("AUDIT_EVENT");
    private final MeterRegistry meterRegistry;

    // Caches for Counters and Timers to avoid recreating them on every invocation
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();
    private final Map<String, Timer> timerCache = new ConcurrentHashMap<>();

    public AuditLogAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private static void addSecurityContextDetails(Map<String, Object> auditDetails) {
        String principalName = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous");
        auditDetails.put("principal", principalName);
        Set<String> roles = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getAuthorities)
                .stream()
                .flatMap(authorities -> authorities.stream().map(GrantedAuthority::getAuthority))
                .collect(Collectors.toCollection(LinkedHashSet::new)); // Use LinkedHashSet for sorted/consistent output
        auditDetails.put("roles", roles);
    }

    @Around("@annotation(com.apenlor.lab.aspects.audit.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime(); // Use nanoTime for Timer accuracy
        String method = joinPoint.getSignature().toShortString();
        String outcome = "SUCCESS"; // Default outcome
        Throwable caughtThrowable = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            outcome = "FAILURE";
            caughtThrowable = throwable;
            throw throwable; // Re-throw to not interfere with normal error handling
        } finally {
            long durationNs = System.nanoTime() - startTime;
            logAndMetric(method, durationNs, outcome, caughtThrowable);
        }
    }

    private void logAndMetric(String method, long durationNs, String outcome, Throwable throwable) {
        logAuditEvent(method, durationNs, outcome, throwable);
        emitAuditMetrics(method, durationNs, outcome);
    }

    private void logAuditEvent(String method, long durationNs, String outcome, Throwable throwable) {
        Map<String, Object> auditDetails = buildAuditDetails(method, durationNs, outcome, throwable);
        AUDIT_LOGGER.atInfo()
                .addMarker(AUDIT_MARKER)
                .addKeyValue("audit", auditDetails)
                .log("Audit Event Recorded");

    }

    private void emitAuditMetrics(String method, long durationNs, String outcome) {
        getCounter(method, outcome).increment();
        getTimer(method, outcome).record(durationNs, TimeUnit.NANOSECONDS);
    }

    /**
     * Orchestrates the building of the complete audit details map by calling
     * specialized helper methods for each type of context.
     */
    private Map<String, Object> buildAuditDetails(String method, long durationNs, String outcome, Throwable throwable) {
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("method", method);
        auditDetails.put("outcome", outcome);
        auditDetails.put("durationMs", durationNs / 1_000_000.0); // Log duration in milliseconds

        addSecurityContextDetails(auditDetails);
        addRequestContextDetails(auditDetails);
        addExceptionDetails(throwable, auditDetails);

        return auditDetails;
    }

    private void addRequestContextDetails(Map<String, Object> auditDetails) {
        getRequest().ifPresentOrElse(
                request -> {
                    auditDetails.put("contextType", "WEB");
                    // Basic IP masking for PII hygiene
                    String remoteAddr = request.getRemoteAddr();
                    auditDetails.put("remoteAddr", sanitizeIpAddress(remoteAddr));
                    auditDetails.put("requestUri", request.getRequestURI());
                    // User-Agent is logged here, but NOT as a metric tag due to high cardinality risk
                    auditDetails.put("userAgent", sanitizeUserAgent(request.getHeader("User-Agent")));
                },
                () -> auditDetails.put("contextType", "NON_WEB")
        );
    }

    private Optional<HttpServletRequest> getRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }

    private void addExceptionDetails(Throwable throwable, Map<String, Object> auditDetails) {
        if (throwable != null) {
            auditDetails.put("exceptionType", throwable.getClass().getSimpleName());
            auditDetails.put("exceptionMessage", sanitizeExceptionMessage(throwable.getMessage()));
            // Capture root cause details for enhanced debuggability
            Throwable rootCause = findRootCause(throwable);
            if (!Objects.equals(rootCause, throwable)) {
                auditDetails.put("rootCauseType", rootCause.getClass().getSimpleName());
                auditDetails.put("rootCauseMessage", sanitizeExceptionMessage(rootCause.getMessage()));
            }
        }
    }

    private Throwable findRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && !Objects.equals(rootCause.getCause(), rootCause)) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private String sanitizeIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown_ip";
        }
        // Replace last octet with 'XXX' for IPv4, or parts of IPv6 for basic masking
        if (ip.contains(".")) { // IPv4
            return ip.substring(0, ip.lastIndexOf('.') + 1) + "XXX";
        } else if (ip.contains(":")) { // IPv6 - simplified masking, more complex for full compliance
            // Mask the last two hextets
            int lastColon = ip.lastIndexOf(':');
            int secondLastColon = ip.lastIndexOf(':', lastColon - 1);
            if (secondLastColon != -1) {
                return ip.substring(0, secondLastColon + 1) + "::XXX";
            }
            return ip + ":XXX"; // Fallback for shorter IPv6
        }
        return ip;
    }

    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "n/a";
        }
        // Truncate long user-agent strings to prevent excessively large logs.
        int maxLength = 256;
        return userAgent.length() > maxLength ? userAgent.substring(0, maxLength - 3) + "..." : userAgent;
    }

    private String sanitizeExceptionMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "n/a";
        }
        // Replace newlines and truncate for log hygiene and to prevent log injection.
        String sanitized = message.replace("\n", "\\n").replace("\r", "\\r");
        int maxLength = 256; // Limit message length to avoid excessively large logs
        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength - 3) + "..." : sanitized;
    }

    private Counter getCounter(String method, String outcome) {
        String cacheKey = "counter:" + method + "::" + outcome;
        return counterCache.computeIfAbsent(cacheKey, k ->
                Counter.builder("app.audit.events.total")
                        .description("Counts the total number of audited events.")
                        .tags("method", method, "outcome", outcome)
                        .register(meterRegistry)
        );
    }

    private Timer getTimer(String method, String outcome) {
        String cacheKey = "timer:" + method + "::" + outcome;
        return timerCache.computeIfAbsent(cacheKey, k ->
                Timer.builder("app.audit.events.duration")
                        .description("Records the duration of audited events.")
                        .tags("method", method, "outcome", outcome)
                        .register(meterRegistry)
        );
    }
}