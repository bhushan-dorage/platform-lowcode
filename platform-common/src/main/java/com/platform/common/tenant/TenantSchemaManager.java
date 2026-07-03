package com.platform.common.tenant;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.regex.Pattern;

/**
 * Manages the lifecycle of per-tenant PostgreSQL schemas.
 *
 * Schema provisioning is intentionally kept outside the normal Flyway boot-time scan
 * because tenant schemas are created on-demand when a tenant is first onboarded.
 *
 * The historyTableName property allows each service to use a distinct Flyway history
 * table so their tenant-scoped migrations (V1__create_entity_tables, V1__create_form_tables,
 * etc.) do not collide when multiple services provision the same tenant schema.
 * Each service should set platform.tenant.flyway-history-table in its application.yml.
 */
@Component
public class TenantSchemaManager {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z0-9_]{1,40}$");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Value("${platform.tenant.flyway-history-table:flyway_schema_history}")
    private String historyTableName;

    public TenantSchemaManager(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * Creates the tenant schema if it does not exist, then applies pending migrations.
     * Safe to call on every tenant boot — Flyway's schema history prevents re-runs.
     */
    public void ensureSchemaExists(String tenantId, TenantTier tier) {
        validateTenantId(tenantId);
        String schema = deriveSchema(tenantId, tier);
        // IF NOT EXISTS avoids a race condition if two pods onboard the same tenant concurrently.
        // The schema name is validated above so the quoted identifier is safe.
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");
        runMigrations(schema, dataSource);
    }

    /**
     * Permanently removes a tenant schema and all its objects.
     * Called only during tenant deprovisioning — irreversible.
     */
    public void dropSchema(String tenantId) {
        validateTenantId(tenantId);
        // STARTER tenants share a schema; callers must guard against calling this for them.
        String schema = tenantId + "_platform";
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
    }

    /**
     * Runs Flyway migrations against a specific schema using this service's history table.
     */
    public void runMigrations(String schema, DataSource targetDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(targetDataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(schema)
                .table(historyTableName)
                .defaultSchema(schema)
                .baselineOnMigrate(false)
                .load();
        flyway.migrate();
    }

    private String deriveSchema(String tenantId, TenantTier tier) {
        return (tier == TenantTier.STARTER) ? "shared_starter" : tenantId + "_platform";
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || !SAFE_IDENTIFIER.matcher(tenantId).matches()) {
            throw new IllegalArgumentException(
                    "Tenant ID must match ^[a-z0-9_]{1,40}$: " + tenantId);
        }
    }
}
