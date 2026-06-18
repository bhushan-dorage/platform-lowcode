# Audit & Compliance Trail

## Audit Event Envelope (all services must use this)
```json
{
  "eventId": "uuid",
  "eventType": "TASK_CLAIMED",
  "domain": "TASK",
  "tenantId": "hsbc",
  "timestamp": "2026-04-26T10:00:00Z",
  "actor": {
    "userId": "u123", "username": "john.doe",
    "roles": ["L1_APPROVER"], "ipAddress": "10.0.0.1", "sessionId": "s456"
  },
  "resource": { "type": "Task", "id": "t789", "name": "Review Application" },
  "action": {
    "operation": "CLAIM",
    "previousState": "UNCLAIMED",
    "newState": "CLAIMED"
  },
  "metadata": {
    "sourceService": "platform-workflow-engine",
    "sourceVersion": "1.0.0",
    "requestId": "req-abc",
    "eventHash": "sha256...",
    "prevEventHash": "sha256..."
  }
}
```

## SHA-256 Event Chaining
```java
String eventHash = DigestUtils.sha256Hex(
    event.getEventId() + "|" + 
    event.getTimestamp() + "|" + 
    objectMapper.writeValueAsString(event.getAction()) + "|" + 
    prevEventHash  // from Redis: {tenantId}:audit:last-hash
);
```

## ClickHouse Schema
```sql
CREATE TABLE audit_events (
    tenant_id LowCardinality(String),
    event_id UUID,
    event_type LowCardinality(String),
    domain LowCardinality(String),
    timestamp DateTime64(3, 'UTC'),
    actor_user_id String,
    actor_username String,
    resource_type LowCardinality(String),
    resource_id String,
    operation String,
    event_hash String,
    prev_event_hash String,
    payload String  -- JSON
) ENGINE = ReplacingMergeTree
PARTITION BY toYYYYMM(timestamp)
ORDER BY (tenant_id, timestamp, event_id);
```

## Tamper-Proof Requirements
- ClickHouse: INSERT-only DB user for audit writer (no UPDATE/DELETE)
- S3: Object Lock enabled, COMPLIANCE mode, 7-year retention
- Daily snapshot job: export previous day → S3 with server-side encryption

## Audit Domains
| Domain      | Events Captured                                      |
|-------------|------------------------------------------------------|
| STUDIO      | Artifact create/edit/approve, git commit             |
| DEPLOYMENT  | Bundle deploy, rollback, env promotion + approvers   |
| PROCESS     | Start, signal, message, terminate                    |
| TASK        | Create, claim, reassign, delegate, complete, escalate|
| DATA        | Field-level before/after on every entity write       |
| ACCESS      | PII field reads (with access_purpose from JWT)       |
| INTEGRATION | Outbound calls, retries, dead-letter                 |
| PRIVILEGED  | Break-glass grants, queries executed, session record |
