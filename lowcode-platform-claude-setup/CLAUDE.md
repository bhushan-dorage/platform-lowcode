# Low-Code Workflow Platform

## Identity
Open-source multi-tenant low-code platform (Appian/ServiceNow alternative).
Architect: Bhushan | Role: Design locked, Claude Code implements only.

## Repo Structure
- One Git repo per microservice — no monorepo
- `platform-bom` — single shared Maven BOM; all services import it
- Each service: `spring-boot-starter-parent` + imports `platform-bom`
- Flyway migrations inside each service under `src/main/resources/db/migration/`

## Stack (LOCKED — never suggest alternatives)
- Java 21 + Spring Boot 3.3 + Maven (single-module per repo, imports platform-bom)
- Flowable 6.8 (BPMN + CMMN + DMN) — multi-tenant Bridge Model
- PostgreSQL 16 + Flyway + Hasura v2 + Redis 7 + Kafka 3.6
- Keycloak 23 + HashiCorp Vault 1.15
- Kubernetes 1.29 + Helm 3 + Istio 1.20
- React 18 + TypeScript + Vite + Zustand (Studio + Portal)
- ClickHouse 23 (audit) + S3 WORM
- Prometheus + Grafana + Jaeger (OTel)

## Critical Rules
1. ALWAYS filter by TenantContext — missing tenant_id filter = P0 bug
2. ALWAYS use cursor-based pagination — never offset
3. ALWAYS return standard envelope: `{ data, meta: { requestId, traceId, timestamp } }`
4. ALWAYS add Micrometer metrics + OTel spans in every service method
5. ALWAYS use Testcontainers for integration tests
6. NEVER suggest redesigning architecture — implement as specified
7. NEVER use optimistic locking for job acquisition — use FOR UPDATE SKIP LOCKED
8. Idempotency-Key header support required on all POST mutation endpoints

## Response Style
- No preamble. Code first.
- No "I'll now create..." — just create.
- Terse comments in code. No obvious comments.
- When done: one-line summary of what was created.

## Quick Reference
→ Architecture details: `.claude/ARCHITECTURE_MAP.md`
→ Common bugs to avoid: `.claude/COMMON_MISTAKES.md`
→ Dev commands: `.claude/QUICK_START.md`
→ Detailed domain docs: `docs/learnings/` (load on demand)
