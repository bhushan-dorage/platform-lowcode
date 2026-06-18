package com.platform.common.tenant;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Manages the lifecycle of per-tenant PostgreSQL schemas.
 *
 * Schema provisioning is intentionally kept outside the normal Flyway boot-time scan
 * because tenant schemas are created on-demand when a tenant is first onboarded.
 */
@Component
public class TenantSchemaManager {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public TenantSchemaManager(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * Creates the tenant schema if it does not exist, then applies pending migrations.
     * Safe to call on every tenant boot — Flyway's schema history prevents re-runs.
     */
    public void ensureSchemaExists(String tenantId, TenantTier tier) {
        String schema = deriveSchema(tenantId, tier);
        // IF NOT EXISTS avoids a race condition if two pods onboard the same tenant concurrently
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");
        runMigrations(schema, dataSource);
    }

    /**
     * Permanently removes a tenant schema and all its objects.
     * Called only during tenant deprovisioning — irreversible.
     */
    public void dropSchema(String tenantId) {
        // We derive the non-STARTER schema; STARTER tenants share a schema and cannot be
        // individually dropped without affecting other tenants — callers must handle that case.
        String schema = tenantId + "_platform";
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
    }

    /**
     * Runs Flyway migrations against a specific schema.
     * Each schema gets its own flyway_schema_history table so migrations are independent.
     */
    public void runMigrations(String schema, DataSource targetDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(targetDataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(schema)
                .table("flyway_schema_history")
                // Default search path ensures DDL statements land in the right schema
                .defaultSchema(schema)
                .baselineOnMigrate(false)
                .load();
        flyway.migrate();
    }

    private String deriveSchema(String tenantId, TenantTier tier) {
        return (tier == TenantTier.STARTER) ? "shared_starter" : tenantId + "_platform";
    }
}
