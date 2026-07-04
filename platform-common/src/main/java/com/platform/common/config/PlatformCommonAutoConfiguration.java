package com.platform.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.JdbcTenantRegistry;
import com.platform.common.tenant.TenantRegistry;
import com.platform.common.tenant.TenantResolutionFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

    /**
     * TenantResolutionFilter reads the authenticated JWT's tenant_id claim off
     * SecurityContextHolder, so it must run after Spring Security's filter chain has
     * authenticated the request — hence an order just past DEFAULT_FILTER_ORDER (-100),
     * the order Spring Boot registers the security filter chain at, rather than
     * HIGHEST_PRECEDENCE.
     */
    @Bean
    public FilterRegistrationBean<TenantResolutionFilter> tenantResolutionFilterRegistration(
            TenantRegistry tenantRegistry, ObjectMapper objectMapper) {
        FilterRegistrationBean<TenantResolutionFilter> registration =
                new FilterRegistrationBean<>(new TenantResolutionFilter(tenantRegistry, objectMapper));
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
        return registration;
    }
}
