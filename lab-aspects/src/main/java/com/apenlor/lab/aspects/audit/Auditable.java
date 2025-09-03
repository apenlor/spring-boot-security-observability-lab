package com.apenlor.lab.aspects.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation for methods that should be audited.
 * <p>
 * When a method is annotated with @Auditable, the AuditLogAspect will intercept
 * its execution, extract relevant security and request context, and write a
 * structured audit log.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
}