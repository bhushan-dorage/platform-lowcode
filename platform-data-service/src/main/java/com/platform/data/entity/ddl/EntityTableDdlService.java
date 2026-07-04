package com.platform.data.entity.ddl;

import com.platform.common.tenant.TenantTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates and alters the real per-entity-type tables backing EntityDefinition/EntityRecord, via
 * validated dynamic DDL through the JdbcTemplate already wired by platform-common's
 * TenantDataSourceConfig (previously unused in this module).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityTableDdlService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * STARTER tenants share one physical schema (shared_starter per ADR-0008), so two different
     * STARTER tenants both defining an entity type called e.g. "invoice" would otherwise collide
     * on the exact same physical table. Mirrors the same tier-based branch TenantContext already
     * uses for schema naming, one level down.
     */
    public String physicalTableName(String tenantId, TenantTier tier, String entityType) {
        String tableName = (tier == TenantTier.STARTER) ? tenantId + "_" + entityType : entityType;
        SqlIdentifiers.validate(tableName, "Table name");
        return tableName;
    }

    public void createTable(String schema, String table, List<FieldSchema> fields) {
        SqlIdentifiers.validate(schema, "Schema name");
        SqlIdentifiers.validate(table, "Table name");
        for (FieldSchema field : fields) {
            SqlIdentifiers.validateFieldName(field.name());
        }

        String columns = fields.stream()
                // Always nullable at the SQL level regardless of the JSON-Schema required flag —
                // "required" is enforced at the application layer on writes going forward; a
                // column can't be NOT NULL at creation time since it never applies retroactively.
                .map(f -> SqlIdentifiers.quote(f.name()) + " " + sqlType(f.type()))
                .collect(Collectors.joining(", "));

        String qualifiedTable = SqlIdentifiers.quote(schema) + "." + SqlIdentifiers.quote(table);
        String createTableSql = "CREATE TABLE " + qualifiedTable + " ("
                + "id UUID PRIMARY KEY DEFAULT gen_random_uuid(), "
                + "tenant_id VARCHAR(64) NOT NULL, "
                + (columns.isEmpty() ? "" : columns + ", ")
                + "archived_at TIMESTAMPTZ, "
                + "created_by VARCHAR(255) NOT NULL, "
                + "created_at TIMESTAMPTZ NOT NULL DEFAULT now(), "
                + "updated_at TIMESTAMPTZ NOT NULL DEFAULT now()"
                + ")";
        jdbcTemplate.execute(createTableSql);

        String indexName = SqlIdentifiers.quote("idx_" + table + "_tenant");
        jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + qualifiedTable
                + " (tenant_id) WHERE archived_at IS NULL");

        log.info("Created entity table schema={} table={} columnCount={}", schema, table, fields.size());
    }

    public void applyAdditiveAlter(String schema, String table, List<FieldSchema> added) {
        if (added.isEmpty()) {
            return;
        }
        SqlIdentifiers.validate(schema, "Schema name");
        SqlIdentifiers.validate(table, "Table name");
        for (FieldSchema field : added) {
            SqlIdentifiers.validateFieldName(field.name());
        }

        String addClauses = added.stream()
                .map(f -> "ADD COLUMN " + SqlIdentifiers.quote(f.name()) + " " + sqlType(f.type()))
                .collect(Collectors.joining(", "));

        String qualifiedTable = SqlIdentifiers.quote(schema) + "." + SqlIdentifiers.quote(table);
        jdbcTemplate.execute("ALTER TABLE " + qualifiedTable + " " + addClauses);

        log.info("Altered entity table schema={} table={} addedColumns={}", schema, table, added.size());
    }

    private String sqlType(String jsonSchemaType) {
        return switch (jsonSchemaType) {
            case "string" -> "TEXT";
            case "number" -> "NUMERIC";
            case "boolean" -> "BOOLEAN";
            case "date" -> "TIMESTAMPTZ";
            case "object", "array" -> "JSONB";
            default -> throw new IllegalArgumentException("Unsupported field type: " + jsonSchemaType);
        };
    }
}
