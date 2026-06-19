CREATE DATABASE IF NOT EXISTS platform_audit;

CREATE TABLE IF NOT EXISTS platform_audit.audit_events (
    event_id        UUID,
    event_type      String,
    domain          String,
    tenant_id       String,
    timestamp       DateTime64(3, 'UTC'),
    actor_user_id   String,
    actor_roles     Array(String),
    actor_ip        String,
    resource_type   String,
    resource_id     String,
    resource_name   String,
    operation       String,
    prev_state      String,
    new_state       String,
    source_service  String,
    request_id      String,
    trace_id        String,
    event_hash      String,
    prev_event_hash String,
    metadata        String  -- JSON blob for extra fields
) ENGINE = ReplacingMergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (tenant_id, domain, timestamp, event_id)
SETTINGS index_granularity = 8192;
