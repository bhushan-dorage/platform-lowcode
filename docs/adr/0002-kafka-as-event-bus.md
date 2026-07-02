# ADR-0002: Apache Kafka as the Internal Event Bus

**Status:** Accepted  
**Date:** 2026-01-15

## Context

The platform needs an asynchronous messaging backbone for:

- Absorbing process-start traffic bursts (async HTTP 202 → Kafka → Flowable consumer)
- Decoupling services: form submission → audit, notification, webhook, analytics pipelines
- Tenant-scoped topic isolation (topic prefix per tenant)
- Durable replay — audit events must never be lost even if the audit-service is temporarily down
- High throughput: target 10,000 concurrent process starts per Enterprise tenant

Candidates evaluated:

| Broker | Notes |
|---|---|
| **Apache Kafka** | Log-based, durable, replay, compaction, Confluent Schema Registry for Avro |
| RabbitMQ | AMQP, good for RPC patterns, weak on replay and large-scale fan-out |
| AWS SQS/SNS | Managed, but vendor lock-in and limited replay (SQS: 14-day max, no compaction) |
| Redis Streams | Lightweight but limited consumer group semantics and no schema registry |
| Pulsar | Strong multi-tenancy model, but smaller ecosystem and operational complexity |

## Decision

Use **Apache Kafka** (Confluent distribution) with **Confluent Schema Registry** for Avro-encoded events.

Topic naming convention: `{tenantKafkaPrefix}.{domain}` where prefix is derived from `TenantContext`.

Key topics:

| Topic | Purpose |
|---|---|
| `{t}.process.events` | Process start requests (burst absorption) |
| `{t}.audit.events` | All domain events fan-in to audit-service |
| `{t}.notifications` | Notification trigger events |
| `{t}.deadletter` | Exhausted-retry events from integration-service |

`TenantAwareKafkaProducer` in `platform-common` automatically prefixes all topic names so individual services never hardcode tenant routing.

## Consequences

**Positive:**
- Log retention enables full replay — audit-service can be rebuilt from the event log
- `FOR UPDATE SKIP LOCKED`-style consumer group semantics avoid duplicate processing
- Schema Registry enforces contract between producers and consumers — breaking changes are caught at publish time
- `auto.create.topics.enable=false` in docker-compose forces explicit topic provisioning, preventing topology drift
- Confluent Cloud available as a drop-in for managed deployment

**Negative:**
- Adds Zookeeper (or KRaft in Kafka 3.3+) as an operational dependency
- Schema Registry is an additional service to operate
- Kafka is overkill for low-throughput Starter-tier tenants — they share the same broker infrastructure
- End-to-end tracing across Kafka boundaries requires manual propagation of `X-Request-ID` / `traceId` in message headers
- Dead-letter handling requires explicit `DeadLetterConsumer` implementation per consumer
