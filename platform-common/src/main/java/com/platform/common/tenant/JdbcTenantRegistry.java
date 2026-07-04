package com.platform.common.tenant;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Default TenantRegistry backed by the platform_meta.tenants table.
 *
 * Called during TenantResolutionFilter, which runs before TenantContext is populated,
 * so the JdbcTemplate routes through TenantRoutingDataSource's platform_meta key
 * (the default datasource). The table is populated by the V1 Flyway migration in
 * platform-common's db/migration/platform_meta location.
 *
 * Results are cached in a ConcurrentHashMap for the lifetime of the process. A restart
 * is required to pick up newly provisioned tenants or tier changes. Services that need
 * live invalidation should declare their own @Bean TenantRegistry.
 */
public class JdbcTenantRegistry implements TenantRegistry {

    private static final String QUERY =
            "SELECT tier FROM platform_meta.tenants WHERE id = ? AND status = 'ACTIVE'";

    private final JdbcTemplate jdbcTemplate;
    private final ConcurrentHashMap<String, TenantTier> cache = new ConcurrentHashMap<>();

    public JdbcTenantRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantTier resolveTier(String tenantId) {
        return cache.computeIfAbsent(tenantId, this::fetchTier);
    }

    private TenantTier fetchTier(String tenantId) {
        try {
            String tier = jdbcTemplate.queryForObject(QUERY, String.class, tenantId);
            return TenantTier.valueOf(tier);
        } catch (EmptyResultDataAccessException ex) {
            throw new TenantNotFoundException(tenantId);
        }
    }
}
