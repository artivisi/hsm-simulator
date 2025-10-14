package com.artivisi.hsm.simulator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class to enable JPA Auditing.
 * This allows automatic population of @CreatedDate and @LastModifiedDate fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
