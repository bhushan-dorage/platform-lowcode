# platform-lowcode: Architecture Review & Comparison vs. Commercial Low-Code Platforms

This document reviews `platform-lowcode` as it exists in this repository today and compares it,
capability by capability, against the commercial low-code/no-code incumbents it explicitly models
itself on — **ServiceNow App Engine, Appian, Microsoft Power Apps, Mendix, and OutSystems**
(per `lowcode-platform-claude-setup/CLAUDE.md`: *"Open-source multi-tenant low-code platform
(Appian/ServiceNow alternative)"*).

## Executive summary

`platform-lowcode` is a genuinely ambitious 18-module polyglot monorepo (11 Spring Boot
microservices, 2 React SPAs, Java/TS SDKs) built around real, non-trivial technology choices:
an embedded Flowable BPMN engine, Apache Camel for integrations, Keycloak/Vault/Kong for
identity and gateway concerns, ClickHouse-backed tamper-evident audit logging, and an ABAC policy
compiler. Several subsystems — audit/compliance, RBAC+ABAC with field masking, the connector
SPI, and AI-assisted page generation — are implemented with a level of care comparable to what
you'd expect from a commercial product.

However, the platform's central promise — *visually design a process, form, page, or data
model and have it run without hand-written code* — is **not fully wired end-to-end today**.
The most consequential gap: a process authored in the visual BPMN designer has no code path that
actually deploys it into the Flowable engine. Several other "visual builder" surfaces (data
modeler, and to a lesser extent forms/pages) save into a separate Git-backed artifact store that
isn't demonstrably connected to the runtime services that are supposed to serve them. The
business-rules service points at an external rules server that isn't part of the shipped stack.
And every module the README calls "core product" — workflow engine, form/page/data services,
entitlements, rules, both Studio apps — is absent from CI and from the one-command
`docker compose up -d` local stack, despite the README's architecture diagram implying they're
all part of the running system.

**Bottom line**: this reads as a well-architected proof-of-concept / architecture showcase with
several production-grade subsystems, not yet a working low-code runtime. The gap between the two
is well-defined and closeable — see the roadmap at the end.

---

## 1. What this project is

An 18-module monorepo:

| Layer | Modules |
|---|---|
| Core product services | `platform-workflow-engine` (8083), `platform-form-service` (8084), `platform-page-service` (8085), `platform-data-service` (8086), `platform-entitlements-service` (8087) |
| Cross-cutting services | `platform-audit-service` (8088), `platform-integration-service` (8089), `platform-notification-service` (8090), `platform-webhook-service` (8091), `platform-rules-service` (undeployed) |
| Frontends | `platform-studio-frontend` (visual builder), `platform-portal-frontend` (end-user runtime) |
| SDKs / shared | `platform-sdk-java`, `platform-sdk-js`, `platform-common`, `platform-bom` |
| Ops/test | `platform-load-tests` (Gatling), `platform-e2e-tests` (Playwright), `infra/` (Kong, Keycloak, Vault, Postgres, ClickHouse, Prometheus/Grafana, K8s HPA, chaos scripts) |

**Tech stack**

| Concern | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Maven (per-service POM importing `platform-bom`) |
| Workflow | Flowable 7 (BPMN 2.0), Redisson distributed locks |
| Rules | KIE Server (Drools/DMN) — called over REST, not embedded |
| Integration | Apache Camel 4 with a pluggable Connector SPI |
| Data | PostgreSQL 16 (schema-per-tenant), Redis 7, ClickHouse 23.12 (audit), Kafka 7.6 + Schema Registry |
| Identity / gateway | Keycloak 23 (OIDC/PKCE), HashiCorp Vault 1.15, Kong 3.6 |
| Frontend | React 18 + TypeScript 5 + Vite 5; `bpmn-js`/`dmn-js` (Studio), `@rjsf/core` (Portal form rendering), `react-dnd`, `zustand` |
| AI | Anthropic Java SDK calling `claude-opus-4-8` for natural-language → page generation |
| Observability | Prometheus, Grafana, Jaeger (OTLP/gRPC), Micrometer |
| Testing | JUnit5/Mockito, Gatling (load), Playwright (E2E, type-checked only in CI), Vitest |

The product vision, per `docs/user-manual.html`, targets Business Analysts, Operations Teams, and
Product Owners building "dashboards, data entry forms, approval workflows, and automated
processes — entirely through configuration, not code" — the same audience commercial low-code
platforms target.

---

## 2. Capability-by-capability comparison

For each area: what the commercial incumbents typically provide, what this repo has today, and a
maturity rating — **Production-grade**, **Partial**, or **Scaffolding only** (implemented in
isolation but not wired into a working end-to-end path).

### Visual workflow / process design — **Partial**

Commercial platforms (Appian, OutSystems, ServiceNow, Mendix) all offer: a visual process
designer, one-click deployment into a running engine, versioned process definitions, and
built-in approval/escalation modeling.

Here: `platform-studio-frontend`'s `BpmnModeler` genuinely embeds `bpmn-js` for drag-and-drop
BPMN authoring, and `platform-workflow-engine` embeds a real Flowable 7 engine with tenant-scoped
task inbox, claim/complete, and Redisson-backed distributed locking for concurrent claims —
non-trivial, working code.

**The gap**: there is no code path connecting the two. `BundleService.deployBundle()`
(`platform-studio-backend/.../BundleService.java:48`) only publishes a Kafka event
(`studio.deploy.events`, line 59) and marks the bundle `DEPLOYING`; nothing in the repository
consumes that topic, and no code anywhere calls Flowable's `RepositoryService.createDeployment`.
A process designed visually in Studio has no mechanism in this codebase to become an executable
process definition. `ProcessStartConsumer` assumes a `processDefinitionKey` is already deployed
by some out-of-band mechanism not present here. There are also no sample `.bpmn`/`.dmn` files
anywhere in the repo, consistent with this path never having been exercised end-to-end.

### Forms — **Partial**

Commercial platforms offer dozens of form field types (repeaters, master-detail, conditional
visibility, wizards, file upload, rich text) with live two-way binding to the data layer.

Here: `platform-form-service` provides real JSON-Schema-based form definitions with versioning,
publish lifecycle, and validation. `FormDesigner` in Studio is a genuine `react-dnd` drag-and-drop
canvas, and the Portal renders forms at runtime via `@rjsf/core` (a real JSON-Schema-driven
renderer, not a custom reimplementation).

**The gap**: the field palette is narrow — 8 basic types (text, number, email, date, select,
checkbox, textarea, section) with no repeaters, master-detail grids, conditional field
visibility, wizards, or file upload. More importantly, Studio-authored form artifacts are saved
to a separate Git-backed artifact store (`platform-studio-backend`), and no code path was found
connecting that store to `platform-form-service`'s own Postgres-backed persistence — two parallel
storage models for the same concept, without a demonstrated bridge between them.

### Pages / dashboards — **Partial, with a genuine bright spot**

Commercial platforms offer rich widget libraries and (increasingly) AI-assisted generation.

Here: `platform-page-service` stores metadata-driven page definitions (JSONB schema) with a
draft→published lifecycle; `PageBuilder` in Studio is a real three-panel (palette/canvas/editor)
drag-and-drop builder producing a `PageSchema` (widgets: kpi, table, form, chart, text);
`PageRenderer` in the Portal renders it live. **`PageGenerationService` calls the real Anthropic
API** (`claude-opus-4-8`) with a detailed system prompt to synthesize a `PageSchema` from a
natural-language description, and the Portal's `PageGenerator` UI shows a live preview before
save (ADR-0011) — this is a genuinely working, differentiating feature, not a stub.

**The gap**: charting is minimal — only bar charts via hand-rolled SVG; the v1.1.0 release notes
explicitly flag line/pie/sparkline as unimplemented. AI generation is scoped only to pages — no
NL→workflow, NL→form, NL→data-model, or NL→rules generation exists.

### Data modeling — **Scaffolding only** (weakest of the "core four")

Commercial platforms (especially OutSystems and Mendix) generate real relational tables per
entity, with foreign keys, joins, indexes, and a query builder.

Here: `platform-data-service`'s migration
(`platform-data-service/src/main/resources/db/migration/tenant/V1__create_entity_tables.sql`)
defines `entity_definitions` (one row per entity type, schema stored as JSONB) and, critically,
a single generic `entity_records` table:

```sql
CREATE TABLE IF NOT EXISTS entity_records (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(64)  NOT NULL,
    entity_type VARCHAR(128) NOT NULL,
    data        JSONB        NOT NULL,
    ...
);
```

Every entity type, regardless of shape, is stored as a JSONB blob in this one table,
discriminated by an `entity_type` column — an EAV/blob model, not real per-entity relational
tables. `HasuraConfigGenerator` produces Hasura GraphQL metadata intended to expose entities via
GraphQL, but Hasura itself is not present anywhere in `docker-compose.yml` or `infra/` —
unexercised scaffolding. The visual `DataModelerPage` in Studio only writes a `DATA_MODEL`
artifact to the Git artifact store; no call from Studio into `platform-data-service`'s actual
entity CRUD API was found — the visual data modeler and the engine it's meant to configure are
disconnected.

### Business rules / decisioning — **Scaffolding only**

Commercial platforms provide visual decision-table authoring backed by an embedded rules engine.

Here: `platform-rules-service`'s `RuleExecutionService` doesn't evaluate rules locally — it
proxies to an external KIE Server via REST, configured with a default of
`http://localhost:8180/kie-server/services/rest`. That KIE server does not exist anywhere in
`docker-compose.yml` or `infra/`, so this service cannot function in the shipped stack. There is
also no dedicated decision-table authoring UI in Studio — only the process-oriented `DmnEditor`,
which is not clearly tied to this service. Notably, `platform-rules-service` is also absent from
the README's own module table and from `docker-compose.yml`, suggesting it's a newer, less
integrated addition.

### Integrations / connectors — **Partial, well-architected core**

Commercial platforms ship hundreds of prebuilt SaaS connectors (Salesforce, SAP, Workday, etc.)
plus a visual connector-configuration experience.

Here: `platform-integration-service` has a genuinely well-designed pluggable Connector SPI
(`ConnectorProvider` interface, `ConnectorProviderRegistry`) building real Apache Camel 4 routes.
Five working connectors exist — HTTP, JDBC, SFTP, Email, Slack — each a real Camel-URI builder,
not a stub, and covered by tests. `platform-webhook-service` is similarly mature: HMAC-SHA256
signed outbound webhooks with 5-attempt exponential backoff and a delivery log.

**The gap**: breadth. Only 5 generic protocol connectors exist versus hundreds of SaaS-specific
prebuilt connectors in commercial platforms; adding a new connector type requires a code change
to the `ConnectorType` enum (ADR-0009), not a runtime-installable registry entry. No purpose-built
Salesforce/SAP/Workday adapters exist despite ADR-0004 aspirationally mentioning them (JDBC could
reach SAP HANA generically, but there's no dedicated adapter).

### Governance: RBAC / ABAC — **Production-grade, with one real security gap**

Here: `platform-entitlements-service` implements a role/permission hierarchy plus a genuinely
interesting ABAC policy compiler (`PolicyCompilerService`) that compiles YAML conditions into
parameterized SQL predicates at activation time, and field-level data masking
(`FieldMaskingService`). This is one of the best-tested subsystems in the repo (3 test files) and
has a real Studio UI (`RoleManagerPage`) for role/permission/user-assignment management.

**A real gap worth flagging**: `TenantResolutionFilter` resolves the active tenant purely from an
`X-Tenant-ID` HTTP header. Nothing found in the codebase (including `infra/kong/kong.yaml`)
cryptographically binds that header to the tenant claim in the caller's Keycloak JWT — tenant
isolation currently rests on trusting a client-supplied header rather than a verified claim, a
materially weaker isolation guarantee than commercial multi-tenant platforms provide.

### Audit / compliance — **Production-grade**

`platform-audit-service` implements a SHA-256 hash-chained, tamper-evident audit trail with SIEM
export (CEF/LEEF/JSON formats) and SOC2/ISO27001/GDPR/HIPAA compliance report generation, backed
by ClickHouse for append-only storage. This is comparable in spirit to ServiceNow's GRC tooling
and is one of the most mature, well-tested parts of the codebase (3 test files, real hash-chain
verification logic).

### ALM / deployment pipeline — **Partial**

Commercial platforms (Appian, OutSystems) provide dev→test→prod environment promotion,
change-set/dependency analysis, and rollback as first-class ALM features.

Here: `platform-studio-backend` provides Git-backed artifact versioning (via JGit,
`GitArtifactStore`) and a draft→published lifecycle with "bundle" packaging. This is a real,
rudimentary version-control layer, but there is no visible staged environment promotion, no
dependency/impact analysis, and no rollback mechanism.

### Multi-tenancy & scalability — **Partial, architecturally sound**

A hybrid schema-per-tenant Postgres model (ADR-0008) — dedicated schemas for Enterprise/
Professional tiers, shared schema + Postgres RLS for Starter — with `TenantSchemaManager`
creating schemas on demand and running per-schema Flyway migrations. Combined with Kafka-driven
async processing and K8s HPA manifests, the architectural direction is sound and comparable to
how commercial SaaS platforms segment tenants by tier. However, K8s HPA manifests exist for only
5 of the ~12 services, and (per the engineering-maturity section below) most of these services
aren't even containerized in the local stack yet, so the multi-tenancy story is unproven beyond
the code level.

### Marketplace / extensibility — **Scaffolding only / absent**

All five commercial platforms have a plugin/component marketplace or template gallery as a core
part of their ecosystem strategy. Nothing equivalent exists here: the Connector SPI is
code-level and requires a rebuild to add a provider, and there is no template gallery for
reusable apps, forms, or pages.

### Mobile / offline — **Absent**

Power Apps, OutSystems, and Mendix all ship native mobile app generation and offline data sync.
This platform is web-SPA only; no mobile-specific rendering layer or offline capability exists.

---

## 3. Engineering maturity snapshot

| Module | Main source files | Test files | Notes |
|---|---|---|---|
| workflow-engine | 33 | 3 | Core services covered; deployment path (see above) is not |
| form-service | 20 | 0 | No tests |
| page-service | 17 | 1 | |
| data-service | 13 | 0 | No tests |
| entitlements-service | 26 | 3 | Best-tested "core" module |
| rules-service | 7 | 0 | No tests; points at nonexistent infra |
| integration-service | 18 | 2 | |
| studio-backend | 21 | 2 | |
| webhook-service | 14 | 3 | |
| notification-service | 18 | 2 | |
| audit-service | 17 | 3 | |
| studio-frontend | — | **0** | Entire visual designer suite (BPMN modeler, Form Designer, Page Builder, Data Modeler, DMN Editor, Role Manager) has zero tests |
| portal-frontend | — | 2 | |

**CI coverage** (`.github/workflows/ci.yml`) builds/tests only: `platform-bom`, `platform-common`,
`platform-audit-service`, `platform-integration-service`, `platform-notification-service`,
`platform-webhook-service`, `platform-portal-frontend`, `platform-sdk-java`, `platform-sdk-js`,
`platform-load-tests`, plus a `lint-infra` job. **Every module the README's own architecture
diagram labels "CORE PRODUCT"** — `platform-workflow-engine`, `platform-form-service`,
`platform-page-service`, `platform-data-service`, `platform-entitlements-service` — **plus
`platform-rules-service` and both Studio apps, are absent from CI entirely.**

**`docker-compose.yml` mirrors this gap**: it declares services only for
`platform-audit-service`, `platform-integration-service`, `platform-notification-service`,
`platform-webhook-service`, and `platform-portal-frontend`, alongside infra (Postgres, Kafka,
Keycloak, Vault, Kong, ClickHouse, observability). The five "CORE PRODUCT" services plus
`rules-service` and both Studio apps have no `Dockerfile` and are not part of the one-command
`docker compose up -d` experience the README's Quick Start implies.

Playwright E2E specs exist (`process-flow.spec.ts`, `portal-flow.spec.ts`, `webhook-flow.spec.ts`,
`audit-flow.spec.ts`) and look reasonable, but CI only runs `tsc --noEmit` against them — they are
never executed against a live stack, so they provide no real regression signal today.

---

## 4. Side-by-side summary matrix

| Capability | ServiceNow | Appian | Power Apps | Mendix | OutSystems | **platform-lowcode** |
|---|---|---|---|---|---|---|
| Visual process design | Mature | Mature | Basic (Power Automate) | Mature | Mature | Designer real; **no deploy path to runtime** |
| One-click deploy to running engine | Yes | Yes | Yes | Yes | Yes | **No — missing entirely** |
| Form builder breadth | Rich | Rich | Rich | Rich | Rich | 8 field types, no repeaters/conditional logic |
| Dashboard/page builder | Rich | Rich | Rich | Rich | Rich | 5 widgets; bar charts only |
| AI-assisted app generation | Emerging | Emerging | Emerging (Copilot) | Emerging | Emerging | **Working NL→page feature**, page-scoped only |
| Dynamic data modeling | Real tables | Real tables | Dataverse (real tables) | Real tables | Real tables | Single generic JSONB table (EAV-style) |
| Business rules/decisioning | Mature | Mature | Basic | Mature (DMN) | Mature | Proxies to a KIE server **not present in the stack** |
| Prebuilt connectors | Hundreds | Hundreds | Hundreds (connectors) | Hundreds | Hundreds | 5 generic protocol connectors, well-architected SPI |
| RBAC / ABAC / field masking | Mature | Mature | Mature (Dataverse security) | Mature | Mature | **Mature** — real ABAC compiler + field masking |
| Audit / compliance reporting | Mature (GRC) | Mature | Mature (Purview) | Mature | Mature | **Mature** — hash-chained, SIEM export, SOC2/ISO/GDPR/HIPAA |
| ALM / environment promotion | Mature | Mature | Mature | Mature | Mature | Git-backed versioning only, no promotion/rollback |
| Marketplace / plugin ecosystem | Large | Large | Large (AppSource) | Large | Large | None |
| Mobile / offline | Yes | Yes | Yes | Yes | Yes | None (web SPA only) |
| Multi-tenancy | Mature | Mature | Mature | Mature | Mature | Sound design (schema-per-tenant + RLS), tenant header not JWT-bound |

---

## 5. Where it genuinely holds its own

It's worth being fair about the parts that are not toy implementations:

- **AI-assisted page generation** — a real, working NL→UI feature via the Anthropic API, which
  most commercial platforms are only beginning to ship in comparable form.
- **Hash-chained, SIEM-exportable audit trail** with automated SOC2/ISO27001/GDPR/HIPAA report
  generation — genuinely comparable to ServiceNow's GRC tooling.
- **ABAC policy compiler** (YAML → parameterized SQL) plus field-level masking — a more
  sophisticated authorization model than many mid-market low-code tools offer out of the box.
- **Pluggable Connector SPI** built on Apache Camel — a clean extension point, even if only 5
  connectors are implemented today.
- **Distributed task-claim locking** (Redisson) for the human-task inbox — handles concurrent
  claim races correctly, a detail many bespoke workflow systems get wrong.

---

## 6. Recommended roadmap to close the gap

Roughly in priority order, based on the concrete gaps found above:

1. **Wire Studio's bundle deploy into Flowable.** Add a consumer for `studio.deploy.events` (or
   call `RepositoryService.createDeployment` directly from `BundleService.deployBundle()`) so a
   process designed in Studio actually becomes runnable. This closes the single biggest functional
   hole in the platform's core value proposition.
2. **Reconcile the two artifact stores.** Decide whether Studio's Git-backed artifact store or
   each service's own Postgres persistence is the source of truth for forms/pages/data models, and
   build the missing publish/sync path — currently they're parallel and disconnected.
3. **Make `platform-rules-service` functional**: either stand up a real KIE server in
   `docker-compose.yml`/`infra/`, or replace the external dependency with an embedded DMN
   evaluator, and add a decision-table authoring UI in Studio.
4. **Give the data service real per-entity storage** — either generate actual relational tables
   per entity (matching Mendix/OutSystems), or finish wiring the Hasura GraphQL layer the code
   already generates config for.
5. **Bind tenant resolution to the verified JWT claim**, not just the client-supplied
   `X-Tenant-ID` header, to close the multi-tenant isolation gap.
6. **Bring the "core product" modules into CI and `docker-compose.yml`** — workflow-engine,
   form-service, page-service, data-service, entitlements-service, rules-service, and both Studio
   apps — so the README's architecture claims are verifiable by anyone running the one-command
   Quick Start, and add unit tests to the currently zero-coverage modules (form-service,
   data-service, rules-service, studio-frontend).
7. **Expand breadth incrementally** once the above are solid: more form field types (repeaters,
   conditional visibility, file upload), more chart types, and a handful of purpose-built SaaS
   connectors (Salesforce, SAP) to start closing the ecosystem gap.
