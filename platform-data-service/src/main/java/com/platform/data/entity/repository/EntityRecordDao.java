package com.platform.data.entity.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.data.entity.ddl.FieldSchema;
import com.platform.data.entity.ddl.SqlIdentifiers;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JdbcTemplate-based CRUD against a real per-entity-type table, replacing the old generic
 * EntityRecordRepository (JPA over one shared entity_records table). Preserves the platform's
 * mandatory cursor-pagination convention exactly (id-ordered, fetch pageSize+1 for hasMore).
 */
@Repository
@RequiredArgsConstructor
public class EntityRecordDao {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public UUID insert(String schema, String table, List<FieldSchema> fields,
                        String tenantId, Map<String, Object> values, String createdBy) {
        String qualifiedTable = qualify(schema, table);
        List<String> columnNames = new ArrayList<>(List.of("tenant_id", "created_by"));
        List<Object> bindValues = new ArrayList<>(List.of(tenantId, createdBy));
        List<String> placeholders = new ArrayList<>(List.of("?", "?"));

        for (FieldSchema field : fields) {
            if (!values.containsKey(field.name())) {
                continue;
            }
            SqlIdentifiers.validateFieldName(field.name());
            columnNames.add(field.name());
            placeholders.add(placeholderFor(field));
            bindValues.add(bindValueFor(field, values.get(field.name())));
        }

        String sql = "INSERT INTO " + qualifiedTable + " ("
                + columnNames.stream().map(SqlIdentifiers::quote).collect(Collectors.joining(", "))
                + ") VALUES (" + String.join(", ", placeholders) + ") RETURNING id";

        return jdbcTemplate.queryForObject(sql, UUID.class, bindValues.toArray());
    }

    public Optional<Map<String, Object>> findById(String schema, String table, String tenantId, UUID id) {
        String qualifiedTable = qualify(schema, table);
        List<Map<String, Object>> results = jdbcTemplate.query(
                "SELECT * FROM " + qualifiedTable + " WHERE id = ? AND tenant_id = ? AND archived_at IS NULL",
                rowMapper(), id, tenantId);
        return results.stream().findFirst();
    }

    public List<Map<String, Object>> findPage(String schema, String table, String tenantId,
                                               UUID cursor, int pageSize) {
        String qualifiedTable = qualify(schema, table);
        // pageSize+1 fetch is the platform-wide cursor-pagination convention — see CursorPage.
        if (cursor != null) {
            return jdbcTemplate.query(
                    "SELECT * FROM " + qualifiedTable
                            + " WHERE tenant_id = ? AND archived_at IS NULL AND id > ? ORDER BY id ASC LIMIT ?",
                    rowMapper(), tenantId, cursor, pageSize + 1);
        }
        return jdbcTemplate.query(
                "SELECT * FROM " + qualifiedTable
                        + " WHERE tenant_id = ? AND archived_at IS NULL ORDER BY id ASC LIMIT ?",
                rowMapper(), tenantId, pageSize + 1);
    }

    public int update(String schema, String table, List<FieldSchema> fields,
                       String tenantId, UUID id, Map<String, Object> values) {
        String qualifiedTable = qualify(schema, table);
        List<String> setClauses = new ArrayList<>();
        List<Object> bindValues = new ArrayList<>();

        for (FieldSchema field : fields) {
            if (!values.containsKey(field.name())) {
                continue;
            }
            SqlIdentifiers.validateFieldName(field.name());
            setClauses.add(SqlIdentifiers.quote(field.name()) + " = " + placeholderFor(field));
            bindValues.add(bindValueFor(field, values.get(field.name())));
        }
        setClauses.add("updated_at = now()");

        String sql = "UPDATE " + qualifiedTable + " SET " + String.join(", ", setClauses)
                + " WHERE id = ? AND tenant_id = ? AND archived_at IS NULL";
        bindValues.add(id);
        bindValues.add(tenantId);

        return jdbcTemplate.update(sql, bindValues.toArray());
    }

    public List<UUID> findIdsForArchival(String schema, String table, String tenantId, Instant cutoff) {
        String qualifiedTable = qualify(schema, table);
        return jdbcTemplate.query(
                "SELECT id FROM " + qualifiedTable
                        + " WHERE tenant_id = ? AND archived_at IS NULL AND created_at < ?",
                (rs, rowNum) -> (UUID) rs.getObject("id"), tenantId, java.sql.Timestamp.from(cutoff));
    }

    public int markArchived(String schema, String table, List<UUID> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String qualifiedTable = qualify(schema, table);
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] args = new Object[ids.size() + 1];
        args[0] = java.sql.Timestamp.from(Instant.now());
        for (int i = 0; i < ids.size(); i++) {
            args[i + 1] = ids.get(i);
        }
        return jdbcTemplate.update(
                "UPDATE " + qualifiedTable + " SET archived_at = ? WHERE id IN (" + placeholders + ")", args);
    }

    private String qualify(String schema, String table) {
        SqlIdentifiers.validate(schema, "Schema name");
        SqlIdentifiers.validate(table, "Table name");
        return SqlIdentifiers.quote(schema) + "." + SqlIdentifiers.quote(table);
    }

    private String placeholderFor(FieldSchema field) {
        return switch (field.type()) {
            case "number" -> "?::numeric";
            case "boolean" -> "?::boolean";
            case "date" -> "?::timestamptz";
            case "object", "array" -> "?::jsonb";
            default -> "?";
        };
    }

    private Object bindValueFor(FieldSchema field, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if ("object".equals(field.type()) || "array".equals(field.type())) {
            try {
                return objectMapper.writeValueAsString(rawValue);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to serialize field '" + field.name() + "' to JSON", e);
            }
        }
        return rawValue;
    }

    /** Maps every column generically; JSONB columns are parsed back into Java objects. */
    private RowMapper<Map<String, Object>> rowMapper() {
        return (ResultSet rs, int rowNum) -> {
            ResultSetMetaData meta = rs.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String columnName = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                if (value instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                    row.put(columnName, parseJson(pgObject.getValue()));
                } else {
                    row.put(columnName, value);
                }
            }
            return row;
        };
    }

    private Object parseJson(String json) {
        try {
            return json == null ? null : objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
