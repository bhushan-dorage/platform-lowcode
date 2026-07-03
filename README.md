# Platform Low-Code Workflow Engine

A production-grade, multi-tenant low-code platform for building, deploying, and operating BPMN-driven workflows with dynamic forms, metadata-driven pages, data modeling, role-based access control, and full observability.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENTS & SDKs                                   │
│                                                                             │
│  ┌─────────────────────┐  ┌──────────────────┐  ┌───────────────────────┐  │
│  │  Portal Frontend    │  │  Studio Frontend │  │  External Systems     │  │
│  │  React 18 + PKCE   │  │  React + BPMN-JS │  │  platform-sdk-java    │  │
│  │  Task Inbox · Forms │  │  Process Design  │  │  @platform/sdk-js     │  │
│  │  Page Builder       │  │  Form Builder    │  │                       │  │
│  └──────────┬──────────┘  └────────┬─────────┘  └──────────┬────────────┘  │
└─────────────┼─────────────────────┼────────────────────────┼───────────────┘
              │                     │                         │
              ▼                     ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     KONG API GATEWAY  :8000                                 │
│              Rate-Limiting · Correlation-ID · JWT Auth · TLS                │
└──┬──────┬──────┬────────┬────────┬────────┬───────┬──────────┬─────────────┘
   │      │      │        │        │        │       │          │
   ▼      ▼      ▼        ▼        ▼        ▼       ▼          ▼

── CORE PRODUCT ─────────────────────────────────────────────────────────────

┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────────────────────────┐
│Workflow│ │  Form  │ │  Page  │ │  Data  │ │     Entitlements Service   │
│ Engine │ │Service │ │Service │ │Service │ │                            │
│        │ │        │ │        │ │        │ │  RBAC + ABAC · Field Mask  │
│Flowable│ │JSON    │ │Metadata│ │Multi-  │ │  Permission enforcement    │
│BPMN    │ │Schema  │ │pages & │ │tenant  │ │                            │
│SLA Mon.│ │Version.│ │widgets │ │Entities│ │                            │
│Redisson│ │Submit  │ │REST API│ │        │ │                            │
│ :8083  │ │ :8084  │ │ :8085  │ │ :8086  │ │          :8087             │
└────────┘ └────────┘ └────────┘ └────────┘ └────────────────────────────┘

── CROSS-CUTTING SERVICES ───────────────────────────────────────────────────

┌────────┐ ┌────────────────────┐ ┌──────────────┐ ┌────────────────────┐
│ Audit  │ │ Integration Service│ │ Notification │ │  Webhook Service   │
│Service │ │                    │ │   Service    │ │                    │
│        │ │ Apache Camel 4     │ │              │ │  HMAC-SHA256       │
│Hash-   │ │ Connector SPI:     │ │ Email · SMS  │ │  Signed Delivery   │
│chain   │ │ HTTP · JDBC · SFTP │ │ Push · InApp │ │  5-attempt retry   │
│SIEM    │ │ Email · Slack      │ │ Kafka-driven │ │  Kafka-driven      │
│ :8088  │ │       :8089        │ │    :8090     │ │      :8091         │
└────────┘ └────────────────────┘ └──────────────┘ └────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          INFRASTRUCTURE LAYER                               │
│                                                                             │
│  ┌──────────────┐  ┌────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  PostgreSQL  │  │   Redis    │  │    Kafka     │  │    Keycloak      │  │
│  │  16-alpine   │  │  7-alpine  │  │  + Zookeeper │  │  23 (OIDC/PKCE)  │  │
│  │  Primary DB  │  │  Sessions  │  │  + Schema    │  │  Multi-realm     │  │
│  │              │  │  Locks     │  │    Registry  │  │  RBAC            │  │
│  └──────────────┘  └────────────┘  └──────────────┘  └──────────────────┘  │
│                                                                             │
│  ┌──────────────┐  ┌────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  ClickHouse  │  │   Vault    │  │   Jaeger     │  │  Prometheus +    │  │
│  │  Audit Store │  │  Secrets   │  │  Tracing     │  │  Grafana         │  │
│  │  23.12       │  │  1.15      │  │  OTLP/gRPC   │  │  Dashboards      │  │
│  └──────────────┘  └────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Module Reference

| Module | Language / Framework | Port | Description |
|---|---|---|---|
| `platform-workflow-engine` | Java 21 / Spring Boot 3 / Flowable | 8083 | BPMN process execution, human task management, distributed locking (Redisson), SLA monitoring |
| `platform-form-service` | Java 21 / Spring Boot 3 | 8084 | JSON Schema form definitions, versioning, validation, submission events |
| `platform-page-service` | Java 21 / Spring Boot 3 | 8085 | Metadata-driven page definitions — JSONB schema, widget catalog, publish lifecycle; AI-assisted generation via Claude API |
| `platform-data-service` | Java 21 / Spring Boot 3 | 8086 | Multi-tenant entity modeling and CRUD with PostgreSQL |
| `platform-entitlements-service` | Java 21 / Spring Boot 3 | 8087 | RBAC + ABAC policy engine, field-level masking, permission enforcement |
| `platform-audit-service` | Java 21 / Spring Boot 3 | 8088 | SHA-256 hash-chained audit trail, SIEM export (CEF/LEEF/JSON), compliance reports (SOC2/ISO27001/GDPR/HIPAA), ClickHouse store |
| `platform-integration-service` | Java 21 / Spring Boot 3 / Camel | 8089 | Connector SPI with 5 pluggable providers: HTTP, JDBC, SFTP, Email, Slack — all building real Apache Camel routes |
| `platform-notification-service` | Java 21 / Spring Boot 3 | 8090 | Kafka-driven multi-channel delivery: email, SMS, push, in-app |
| `platform-webhook-service` | Java 21 / Spring Boot 3 | 8091 | HMAC-SHA256 signed outbound webhooks, 5-attempt exponential-backoff retry |
| `platform-studio-backend` | Java 21 / Spring Boot 3 | — | Git-backed artifact store, BPMN/form deployment pipeline |
| `platform-portal-frontend` | React 18 / TypeScript / Vite | 3001 | SPA: Keycloak PKCE login, real-time task inbox, dynamic form rendering, metadata-driven page renderer (`/pages/:pageKey`) |
| `platform-studio-frontend` | React 18 / BPMN-JS / DMN-JS | — | Visual process designer, drag-and-drop form builder, and visual drag-and-drop Page Builder |
| `platform-sdk-java` | Java 21 / Maven multi-module | — | `platform-sdk-core`, `platform-sdk-process`, `platform-sdk-task`, `spring-boot-starter` |
| `platform-sdk-js` | TypeScript / Node ≥ 18 | — | `@platform/sdk-js` — entity API client, code generator |
| `platform-common` | Java 21 | — | Shared response envelopes, cursor pagination, tenant context, Kafka producer |
| `platform-bom` | Maven BOM | — | Centralized dependency version management |
| `platform-load-tests` | Scala / Gatling 3.9.5 | — | 4 load simulations (burst, inbox, claim/complete, form submit) |
| `platform-e2e-tests` | TypeScript / Playwright 1.44 | — | End-to-end browser tests for portal and studio flows |

---

## Technology Stack

### Core Runtime
- **Java 21** — all backend services
- **Spring Boot 3.3** — web, security, actuator, data JPA, validation
- **Flowable 7** — BPMN 2.0 process engine
- **Apache Camel 4** — pluggable integration routes (HTTP, JDBC, SFTP via camel-ftp, Email, Slack)
- **Redisson** — distributed locks over Redis

### Data Stores
- **PostgreSQL 16** — primary relational store (all services); schema-per-tenant multi-tenancy
- **Redis 7** — session cache, distributed locks
- **ClickHouse 23.12** — append-only audit event store
- **Kafka 7.6 (Confluent)** — event streaming with Schema Registry

### Security & Identity
- **Keycloak 23** — OIDC/OAuth2, PKCE for SPAs, client credentials for services
- **HashiCorp Vault 1.15** — secrets management
- **Kong 3.6** — API gateway (JWT validation, rate-limiting, TLS termination)

### Frontend
- **React 18** + **TypeScript 5** + **Vite 5**
- **BPMN-JS / DMN-JS** — process and decision model visualization
- **@rjsf/core** — JSON Schema-driven form rendering
- **PageRenderer** — metadata-driven page engine: KPI, table, chart, form, text widgets
- **PageGenerator** — AI-assisted page creation: natural language prompt → PageSchema via Claude API (`claude-opus-4-8`)
- **Visual Page Builder** — drag-and-drop Studio tool for teams without AI access; produces the same PageSchema, no LLM required

### Observability
- **Prometheus + Grafana 10** — metrics and dashboards
- **Jaeger 1.56** — distributed tracing (OTLP/gRPC)
- **Spring Boot Actuator** — health, info, metrics endpoints

### Testing
- **JUnit 5 + Mockito 5 + AssertJ** — unit and integration tests
- **Gatling 3.9.5 (Scala)** — load tests (500 VU burst, 200 VU inbox, 100 VU task claim, 300 VU form submit)
- **Playwright 1.44 (TypeScript)** — E2E browser tests
- **Vitest + Testing Library** — React component tests

### Infrastructure
- **Docker Compose** — local full-stack environment
- **Kubernetes HPA v2** — CPU + custom Kafka-lag and Flowable pending-jobs metrics
- **Chaos scripts** — pod-kill recovery, stale-lock detection

---

## Quick Start

### Prerequisites

- Docker Desktop / Docker Engine with Compose v2
- Java 21 (for building services)
- Node.js 20+ (for frontend and JS SDK)
- Maven 3.9+

### Start the full stack

```bash
docker compose up -d
```

Services start in dependency order. Wait for Keycloak (~60 s) before running services.

| UI | URL |
|---|---|
| Kong API Gateway | http://localhost:8000 |
| Keycloak Admin | http://localhost:8080 — admin / admin |
| Kafka UI | http://localhost:8082 |
| Vault | http://localhost:8200 — token: `root-token` |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 — admin / admin |
| Jaeger | http://localhost:16686 |

### Build a service

```bash
# Install shared dependencies first
mvn -B install -DskipTests -f platform-bom/pom.xml
mvn -B install -DskipTests -f platform-common/pom.xml

# Build any service (example)
mvn -B verify -f platform-workflow-engine/pom.xml
mvn -B verify -f platform-page-service/pom.xml
```

### Build and run the portal frontend

```bash
cd platform-portal-frontend
npm ci
npm run dev      # http://localhost:3001
```

### Build the Java SDK

```bash
cd platform-sdk-java
mvn -B verify
```

### Run load tests

```bash
cd platform-load-tests
mvn -B gatling:test -Dgatling.simulationClass=simulations.ProcessStartBurstSimulation
```

---

## API

The full OpenAPI 3.1 specification is at [`docs/openapi.yaml`](docs/openapi.yaml).  
Base URL (local): `http://localhost:8000`

All endpoints require a Keycloak Bearer token:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/platform/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=platform-backend&client_secret=<secret>" \
  | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/processes
```

### Key endpoints

| Method | Path | Service | Description |
|---|---|---|---|
| `POST` | `/api/v1/processes` | workflow-engine | Start a process instance |
| `GET` | `/api/v1/tasks?assignee={user}` | workflow-engine | List tasks in inbox |
| `POST` | `/api/v1/tasks/{id}/claim` | workflow-engine | Claim a task |
| `POST` | `/api/v1/tasks/{id}/complete` | workflow-engine | Complete a task with variables |
| `GET` | `/api/v1/forms/{key}/latest` | form-service | Get published form schema |
| `POST` | `/api/v1/forms/{key}/submissions` | form-service | Submit a form |
| `POST` | `/api/v1/pages/generate` | page-service | AI-generate a page definition from a natural language prompt |
| `POST` | `/api/v1/pages` | page-service | Create a page definition |
| `GET` | `/api/v1/pages/{key}` | page-service | Get page schema (used by `PageRenderer`) |
| `POST` | `/api/v1/pages/{key}/publish` | page-service | Publish a page (DRAFT → PUBLISHED) |
| `GET` | `/api/v1/entities/{type}` | data-service | List entity records |
| `GET` | `/api/v1/audit/events` | audit-service | Query immutable audit log |
| `POST` | `/api/v1/webhooks/registrations` | webhook-service | Register a webhook endpoint |

---

## Page Schema Format

A page definition stores a JSONB schema describing sections and widgets:

```json
{
  "version": "1.0",
  "title": "Order Management",
  "description": "Monitor and manage customer orders",
  "layout": {
    "type": "sections",
    "sections": [
      {
        "id": "metrics",
        "title": "Key Metrics",
        "columns": 3,
        "widgets": [
          { "id": "w1", "type": "kpi", "colSpan": 1, "title": "Open Orders",
            "config": { "label": "Open Orders", "dataSource": { "url": "/api/v1/entities/order", "valueField": "count" } } },
          { "id": "w2", "type": "chart", "colSpan": 2, "title": "Orders by Day",
            "config": { "chartType": "bar", "dataSource": { "url": "/api/v1/metrics/orders-by-day", "labelField": "day", "valueField": "count" } } }
        ]
      },
      {
        "id": "actions",
        "columns": 2,
        "widgets": [
          { "id": "w3", "type": "form", "colSpan": 1, "title": "New Order",
            "config": { "formKey": "create-order", "submitUrl": "/api/v1/entities/order" } },
          { "id": "w4", "type": "text", "colSpan": 1,
            "config": { "content": "Orders after 3 PM EST ship next day.", "variant": "warning" } }
        ]
      }
    ]
  }
}
```

**Widget types**: `kpi` · `table` · `form` (reuses `platform-form-service` definitions) · `chart` · `text`

The portal renders any published page at `/pages/{pageKey}` — no code deployment required for new screens.

---

## Event Flow

```
User Action
    │
    ▼
Kong Gateway  ──── JWT validated ────►  Backend Service
                                              │
                                    emits Kafka event
                                              │
                        ┌─────────────────────┴──────────────────────┐
                        ▼                                             ▼
               Audit Service                             Notification Service
               writes to ClickHouse                      fans out to channels
                        │                                             │
               SHA-256 hash chain                         Email · SMS · Push · In-App
                                                                      │
                                                             Webhook Service
                                                        signs with HMAC-SHA256
                                                         delivers to subscriber
```

---

## CI / CD

GitHub Actions runs parallel jobs on every push to `main` or `claude/**` branches and on pull requests targeting `main`.

| Job | What it does |
|---|---|
| `build-platform-bom` | Installs the Maven BOM |
| `build-platform-common` | Tests shared library (Postgres + Redis services) |
| `build-platform-audit-service` | Builds and tests audit service |
| `build-platform-integration-service` | Builds integration service — 15 tests including Connector SPI |
| `build-platform-notification-service` | Builds and tests notification service |
| `build-platform-webhook-service` | Builds and tests webhook service |
| `build-portal-frontend` | `npm ci` → vitest → `tsc && vite build` |
| `build-sdk-java` | Maven multi-module verify (4 modules) |
| `build-sdk-js` | `npm ci` → jest → `tsc` build |
| `build-load-tests` | Compiles Gatling Scala simulations |
| `build-e2e-tests` | `npm ci` → `tsc --noEmit` type-check |
| `lint-infra` | `docker compose config` + Kong YAML parse |

---

## Security Model

- **Authentication**: Keycloak OIDC. SPAs use PKCE; services use client credentials.
- **Authorization**: RBAC (role-bound) + ABAC (attribute-bound) policies enforced by `platform-entitlements-service`. Field-level masking for sensitive data (PII, PCI).
- **Transport**: TLS terminated at Kong for all inbound traffic.
- **Webhook integrity**: Every outbound webhook is signed with HMAC-SHA256 (`X-Platform-Signature` header). Receivers verify the signature before processing.
- **Audit integrity**: Each audit event carries a SHA-256 hash of the previous event — producing a tamper-evident hash chain stored in ClickHouse.
- **Secrets**: All credentials stored in HashiCorp Vault; services fetch at startup.
- **Input validation**: All SQL uses parameterized queries; JSON Schema validation on form submissions; request size capped at 10 MB at the gateway.

---

## Repository Layout

```
platform-lowcode/
├── .github/workflows/ci.yml        # CI pipeline
├── docker-compose.yml              # Full local stack
├── infra/
│   ├── k8s/hpa/                    # Kubernetes HPA manifests
│   ├── kong/kong.yaml              # API gateway declarative config
│   ├── keycloak/realm.json         # Pre-configured Keycloak realm
│   ├── postgres/init.sql           # Schema bootstrap
│   ├── clickhouse/init.sql         # Audit table DDL
│   ├── prometheus/prometheus.yml   # Scrape config
│   ├── grafana/provisioning/       # Dashboard provisioning
│   ├── vault/init.sh               # Vault secret seeding
│   └── chaos/                      # Chaos test scripts
├── docs/
│   ├── openapi.yaml                # Full OpenAPI 3.1 specification
│   ├── adr/                        # Architecture Decision Records (ADR-0001 – ADR-0012)
│   ├── operator-runbook.md         # Day-2 operations guide
│   ├── tenant-onboarding.md        # Tenant setup playbook
│   ├── release-notes-v1.0.0.md    # v1.0.0 release notes
│   └── release-notes-v1.1.0.md    # v1.1.0 release notes
├── platform-bom/                   # Maven BOM (dependency versions)
├── platform-common/                # Shared library (envelopes, pagination, tenant)
├── platform-workflow-engine/       # BPMN runtime + task management
├── platform-form-service/          # JSON Schema form management
├── platform-page-service/          # Metadata-driven page definitions
├── platform-data-service/          # Multi-tenant entity store
├── platform-entitlements-service/  # RBAC + ABAC access control
├── platform-audit-service/         # Immutable hash-chained audit trail
├── platform-integration-service/   # Apache Camel — Connector SPI + 5 providers
├── platform-notification-service/  # Kafka-driven multi-channel notifications
├── platform-webhook-service/       # HMAC-SHA256 signed outbound webhooks
├── platform-studio-backend/        # Git-backed artifact store + deployment
├── platform-studio-frontend/       # Visual process and form designer
├── platform-portal-frontend/       # End-user portal SPA + PageRenderer
├── platform-sdk-java/              # Java SDK (Maven multi-module)
├── platform-sdk-js/                # TypeScript/JS SDK
├── platform-load-tests/            # Gatling simulations
└── platform-e2e-tests/             # Playwright E2E tests
```

---

## Documentation

| Document | Location |
|---|---|
| **User Manual** (Business Analysts) | [`docs/user-manual.html`](docs/user-manual.html) |
| API Reference (OpenAPI 3.1) | [`docs/openapi.yaml`](docs/openapi.yaml) |
| Architecture Decision Records | [`docs/adr/`](docs/adr/README.md) (ADR-0001 – ADR-0012) |
| Operator Runbook | [`docs/operator-runbook.md`](docs/operator-runbook.md) |
| Tenant Onboarding | [`docs/tenant-onboarding.md`](docs/tenant-onboarding.md) |
| Release Notes v1.0.0 | [`docs/release-notes-v1.0.0.md`](docs/release-notes-v1.0.0.md) |
| Release Notes v1.1.0 | [`docs/release-notes-v1.1.0.md`](docs/release-notes-v1.1.0.md) |

---

## Performance

Load targets validated with Gatling 3.9.5:

| Scenario | VUs | Target |
|---|---|---|
| Process start burst | 500 | 10,000 concurrent starts per Enterprise tenant |
| Task inbox load | 200 | p99 < 500 ms |
| Task claim/complete | 100 | Zero `TASK_ALREADY_CLAIMED` conflicts |
| Form submission | 300 | p99 < 2 s |
