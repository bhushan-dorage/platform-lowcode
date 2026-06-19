package com.platform.audit.repository;

import com.platform.audit.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AuditRepository {

    private static final String INSERT_SQL = """
            INSERT INTO platform_audit.audit_events
            (event_id, event_type, domain, tenant_id, timestamp, actor_user_id, actor_roles,
             actor_ip, resource_type, resource_id, resource_name, operation, prev_state, new_state,
             source_service, request_id, trace_id, event_hash, prev_event_hash, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate clickhouseJdbc;

    private final ConcurrentHashMap<String, String> latestHashCache = new ConcurrentHashMap<>();

    /**
     * Returns the latest event hash for the tenant.
     *
     * Checks the in-memory cache first to avoid a round-trip for every event.
     * Falls back to ClickHouse on a cold start or after a restart.
     * Returns null if no events have been recorded for this tenant yet.
     */
    public String getLatestHash(String tenantId) {
        String cached = latestHashCache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        try {
            List<Map<String, Object>> rows = clickhouseJdbc.queryForList(
                    "SELECT event_hash FROM platform_audit.audit_events WHERE tenant_id = ? ORDER BY timestamp DESC LIMIT 1",
                    tenantId);
            if (rows.isEmpty()) {
                return null;
            }
            return (String) rows.get(0).get("event_hash");
        } catch (Exception e) {
            log.warn("Failed to fetch latest hash for tenant {} from ClickHouse: {}", tenantId, e.getMessage());
            return null;
        }
    }

    /**
     * Updates the in-memory cache with the most recently computed hash for the tenant.
     */
    public void updateLatestHash(String tenantId, String hash) {
        latestHashCache.put(tenantId, hash);
    }

    /**
     * Inserts a fully-hydrated audit event (with computed hash chain fields) into ClickHouse.
     *
     * The actor_roles Array(String) column is passed as a String[] because the ClickHouse
     * JDBC driver maps Java String arrays to ClickHouse Array(String).
     */
    public void insert(AuditEvent event, String eventHash, String prevEventHash) {
        try {
            clickhouseJdbc.update(INSERT_SQL,
                    event.getEventId(),
                    event.getEventType(),
                    event.getDomain(),
                    event.getTenantId(),
                    Timestamp.from(event.getTimestamp()),
                    event.getActorUserId(),
                    event.getActorRoles() != null ? event.getActorRoles().toArray(new String[0]) : new String[0],
                    event.getActorIp(),
                    event.getResourceType(),
                    event.getResourceId(),
                    event.getResourceName(),
                    event.getOperation(),
                    event.getPrevState(),
                    event.getNewState(),
                    event.getSourceService(),
                    event.getRequestId(),
                    event.getTraceId(),
                    eventHash,
                    prevEventHash,
                    event.getMetadataAsJson());
        } catch (Exception e) {
            log.error("Failed to insert audit event {} for tenant {}: {}",
                    event.getEventId(), event.getTenantId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to persist audit event: " + event.getEventId(), e);
        }
    }

    /**
     * Queries audit events for a tenant with optional time-range, domain, and operation filters.
     *
     * Results are always ordered newest-first and capped at 1 000 rows regardless of the
     * requested limit, to protect ClickHouse from runaway scans.
     */
    public List<Map<String, Object>> query(String tenantId,
                                           String from,
                                           String to,
                                           String domain,
                                           String operation,
                                           int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM platform_audit.audit_events WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        if (from != null && !from.isBlank()) {
            sql.append(" AND timestamp >= ?");
            params.add(from);
        }
        if (to != null && !to.isBlank()) {
            sql.append(" AND timestamp <= ?");
            params.add(to);
        }
        if (domain != null && !domain.isBlank()) {
            sql.append(" AND domain = ?");
            params.add(domain);
        }
        if (operation != null && !operation.isBlank()) {
            sql.append(" AND operation = ?");
            params.add(operation);
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ").append(Math.min(limit, 1000));

        return clickhouseJdbc.queryForList(sql.toString(), params.toArray());
    }
}
