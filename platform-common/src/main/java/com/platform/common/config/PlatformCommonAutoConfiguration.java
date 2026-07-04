package com.platform.common.config;

import com.platform.common.tenant.JdbcTenantRegistry;
import com.platform.common.tenant.TenantRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring Boot auto-configuration entry point for platform-common.
 *
 * Registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * so that every service that has platform-common on its classpath gets the tenant resolution
 * filter, routing datasource, idempotency filter, and Kafka producer registered automatically —
 * without needing scanBasePackages on the individual service's @SpringBootApplication.
 *
 * The JdbcTenantRegistry bean is conditional: services that need a different TenantRegistry
 * implementation (e.g., a cache-backed one) can declare their own @Bean and this default
 * will not be registered.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.platform.common")
public class PlatformCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantRegistry.class)
    public TenantRegistry jdbcTenantRegistry(JdbcTemplate jdbcTemplate) {
        return new JdbcTenantRegistry(jdbcTemplate);
    }
}
