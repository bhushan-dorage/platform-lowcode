# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for the Platform Low-Code project.

Each ADR documents a significant architectural decision: the context that forced the decision, the options considered, what was decided, and the trade-offs accepted.

## Index

| ADR | Title | Status |
|---|---|---|
| [0001](0001-flowable-as-bpmn-engine.md) | Flowable as the BPMN Process Engine | Accepted |
| [0002](0002-kafka-as-event-bus.md) | Apache Kafka as the Internal Event Bus | Accepted |
| [0003](0003-clickhouse-for-audit-storage.md) | ClickHouse for Audit Event Storage | Accepted |
| [0004](0004-apache-camel-for-integration.md) | Apache Camel for the Integration Engine | Accepted |
| [0005](0005-keycloak-for-identity.md) | Keycloak as the Identity and Access Management Provider | Accepted |
| [0006](0006-redisson-for-distributed-locking.md) | Redisson for Distributed Locking | Accepted |
| [0007](0007-kong-as-api-gateway.md) | Kong as the API Gateway | Accepted |
| [0008](0008-postgresql-schema-per-tenant.md) | PostgreSQL Schema-per-Tenant Multi-Tenancy Model | Accepted |
| [0009](0009-connector-spi.md) | Connector SPI for Integration Service | Accepted |

## Format

Each ADR follows this structure:

- **Status** — `Proposed` / `Accepted` / `Deprecated` / `Superseded by ADR-XXXX`
- **Context** — the problem, constraints, and requirements that drove the decision
- **Decision** — what was decided and how it is implemented
- **Consequences** — positive outcomes and accepted trade-offs

## Adding a New ADR

1. Copy an existing ADR as a template
2. Number it sequentially (`0009-...`)
3. Add it to the index above
4. Set status to `Proposed` until reviewed; change to `Accepted` once merged
