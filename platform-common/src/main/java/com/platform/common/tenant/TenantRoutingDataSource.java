package com.platform.common.tenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Routes JDBC connections to the correct schema/datasource based on the current TenantContext.
 *
 * Key semantics:
 *   - STARTER tenants share the "shared_starter" datasource entry.
 *   - ENTERPRISE/PROFESSIONAL get a dedicated entry keyed by their schema name ("{tenantId}_platform").
 *   - When TenantContext is absent (e.g. during application startup or background tasks), falls
 *     back to "platform_meta" which holds cross-tenant administrative tables.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    /** Sentinel key for the cross-tenant administrative datasource. */
    public static final String PLATFORM_META_KEY = "platform_meta";

    @Override
    protected Object determineCurrentLookupKey() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null) {
            // Fallback for startup migrations, health checks, and background jobs
            return PLATFORM_META_KEY;
        }
        return ctx.schema();
    }

    @Override
    public void afterPropertiesSet() {
        // Ensure the BOM key exists so AbstractRoutingDataSource does not reject
        // the fallback path during initialisation validation.
        // The actual datasource map is populated by TenantDataSourceConfig.
        super.afterPropertiesSet();
    }
}
