# ADR-0003: ClickHouse for Audit Event Storage

**Status:** Accepted  
**Date:** 2026-01-15

## Context

The audit service needs to store and query a high-volume, append-only stream of audit events with these characteristics:

- Write-heavy: every API mutation across all services produces an audit event
- Immutable: events must never be updated or deleted (tamper-evident)
- SHA-256 hash chain: each event stores the hash of the previous event — sequential write order must be preserved per tenant
- Long retention: 7-year retention for compliance (SOC2, HIPAA, GDPR, ISO27001)
- Query patterns: filter by tenant, date range, actor, domain, event type; aggregate counts for compliance reports
- SIEM export: bulk export in CEF/LEEF/JSON format

Candidates evaluated:

| Store | Notes |
|---|---|
| **ClickHouse** | Columnar, OLAP-optimised, `ReplacingMergeTree`, sub-second aggregations over billions of rows |
| PostgreSQL (main DB) | Already in use, but OLTP row storage is inefficient for time-series scan queries at scale |
| TimescaleDB | PostgreSQL extension, good for time-series, but still row-based compression vs. columnar |
| Elasticsearch / OpenSearch | Excellent full-text search, but overkill for structured audit; costly at scale |
| AWS S3 + Athena | Cheap long-term storage, but query latency is high (minutes not milliseconds) |
| Apache Cassandra | Wide-column, good write throughput, but weak for ad-hoc aggregation queries |

## Decision

Use **ClickHouse 23.x** as the dedicated audit event store.

Schema (`platform_audit.audit_events`):
- Engine: `ReplacingMergeTree` partitioned by `toYYYYMM(timestamp)`, ordered by `(tenant_id, domain, timestamp, event_id)`
- Fields include `event_hash` (SHA-256 of this event) and `prev_event_hash` (SHA-256 of the previous event) for the tamper-evident chain
- No ORM — raw JDBC via `ClickHouseConfig` to keep write paths minimal and avoid N+1 on bulk inserts

For long-term WORM compliance, monthly partitions are exported to S3-compatible storage with object lock enabled.

## Consequences

**Positive:**
- Columnar storage compresses audit events 10–20× better than row stores
- `ORDER BY (tenant_id, domain, timestamp)` makes tenant-scoped time-range queries extremely fast
- `ReplacingMergeTree` handles idempotent re-inserts from Kafka consumer retries without duplicates
- ClickHouse Cloud available for managed deployment
- Partition-level TTL can enforce retention policy without full table scans

**Negative:**
- ClickHouse is an additional operational dependency (separate from PostgreSQL)
- No JPA/Hibernate support — all queries are raw SQL, requiring more care with query construction
- `ReplacingMergeTree` deduplication is eventual (background merges) — short windows may show duplicates in `SELECT`; work around with `FINAL` keyword or `argMax` aggregation
- Hash chain verification requires sequential reads per tenant, which is a sequential scan not suited for random access
- Local dev requires the ClickHouse Docker container (~1 GB image), adding to compose startup time
