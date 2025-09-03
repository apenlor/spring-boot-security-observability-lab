package com.apenlor.lab.aspects.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The main Spring configuration class for the lab-aspects module.
 * <p>
 * By importing this single class into an application, the application's
 * component scan will be instructed to also scan the base package of this
 * module discovering and registering all components within it, such as the
 * AuditLogAspect.
 */
@Configuration
@ComponentScan("com.apenlor.lab.aspects")
public class AspectsModuleConfig {
}