# ADR-0001: Flowable as the BPMN Process Engine

**Status:** Accepted  
**Date:** 2026-01-15

## Context

The platform requires a BPMN 2.0-compliant process engine to orchestrate long-running human workflows. Key requirements:

- BPMN 2.0 process execution with human tasks, service tasks, gateways, timers, and boundary events
- Multi-tenant process isolation (dedicated engines per Enterprise tenant)
- Distributed deployment — engine must survive pod restarts without losing in-flight instances
- PostgreSQL as the persistence store (no vendor-specific DB requirement)
- Embeddable as a Spring Boot library, not a separate server to operate
- Active open-source community and commercially supported enterprise edition path

Candidates evaluated:

| Engine | Notes |
|---|---|
| **Flowable** | Apache 2.0, Spring-native embedding, PostgreSQL-first, active fork of Activiti 5 |
| Camunda Platform 7 | Strong tooling, but requires separate engine server (not embeddable in the same way post-v7) |
| Camunda 8 (Zeebe) | Cloud-native, but uses its own Zeebe broker — not PostgreSQL-backed; large operational footprint |
| jBPM / KIE | Red Hat ecosystem, heavier dependency tree, tighter coupling to Drools |
| Activiti | Original codebase; Flowable is the more actively maintained fork |

## Decision

Use **Flowable 6.x** embedded as a Spring Boot library within `platform-workflow-engine`.

- One `ProcessEngine` bean per Enterprise tenant, each pointing to its own PostgreSQL schema (`{tenantId}_platform`)
- Starter-tier tenants share a single engine using Flowable's native `tenantId` column on all `act_*` tables
- History level set to `FULL` for Enterprise, `ACTIVITY` for Starter (cost vs. auditability trade-off)
- Job executor uses PostgreSQL `FOR UPDATE SKIP LOCKED` to prevent acquisition storms under load

## Consequences

**Positive:**
- No separate process server to operate — engine runs inside the Spring Boot pod
- Schema-per-tenant isolation is native via `ProcessEngineConfiguration.setDatabaseSchema()`
- Flowable's `act_ru_job` / `act_ru_deadletter_job` tables give built-in retry and dead-letter handling
- Large SQL-based history tables in PostgreSQL enable complex audit queries without a separate store
- Apache 2.0 licence; Flowable Enterprise licence available if commercial support is needed

**Negative:**
- Flowable tables (`act_re_*`, `act_ru_*`, `act_hi_*`) add ~40 tables per tenant schema — schema sprawl at high tenant counts
- Horizontal scaling requires all pods to share the same PostgreSQL schema; job acquisition contention increases beyond ~10 pods without tuning
- BPMN modelling tooling (bpmn-js in Studio) must be kept compatible with Flowable's supported BPMN 2.0 subset
- Migrating away would require exporting all in-flight instances and re-importing into a new engine — high cost
