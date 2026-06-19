# Architecture Map

## Repo Layout — One Repo Per Service

Each service is an independent Git repository. All share a single BOM repo for version alignment.

```
github.com/org/
  platform-bom/                 ← SINGLE Maven BOM pom.xml (all versions locked here)
  platform-common/              ← TenantContext, AbstractRoutingDataSource, TenantAwareKafkaProducer
  platform-gateway/             ← Kong declarative config + Lua plugins
  platform-workflow-engine/     ← Flowable engine service (REST API + Kafka consumers)
  platform-form-service/        ← Form def CRUD + JSON Schema validation + submission
  platform-data-service/        ← Dynamic entity CRUD + archival engine + Hasura config
  platform-rules-service/       ← KIE Server wrapper (DMN + DRL)
  platform-integration-service/ ← Apache Camel routes + connector registry
  platform-audit-service/       ← Kafka → ClickHouse + SHA-256 chaining + WORM export
  platform-notification-service/← Email(SendGrid)/SMS(Twilio)/Push(FCM)
  platform-studio-backend/      ← Artifact store (JGit) + bundle packager + CI/CD trigger
  platform-studio-frontend/     ← React + BPMN.js + DMN.js + react-dnd form designer
  platform-portal-frontend/     ← React Portal (task inbox + dynamic forms + dashboards)
  platform-sdk-java/            ← Java SDK (single Maven project, no parent monorepo)
  platform-sdk-js/              ← TypeScript SDK (npm)
  platform-infra/               ← Helm charts + K8s manifests + Terraform
```

## BOM Structure (platform-bom repo)
```xml
<!-- platform-bom/pom.xml — published to Artifactory as com.platform:platform-bom:{version} -->
<project>
  <groupId>com.platform</groupId>
  <artifactId>platform-bom</artifactId>
  <packaging>pom</packaging>
  <dependencyManagement>
    <dependencies>
      <!-- Spring Boot -->
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>3.3.x</version>
        <type>pom</type><scope>import</scope>
      </dependency>
      <!-- Platform internals -->
      <dependency>
        <groupId>com.platform</groupId>
        <artifactId>platform-common</artifactId>
        <version>${platform.common.version}</version>
      </dependency>
      <!-- Flowable, Kafka, Redis, etc. — all pinned here -->
    </dependencies>
  </dependencyManagement>
</project>
```

## Each Service pom.xml Pattern
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.x</version>
</parent>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.platform</groupId>
      <artifactId>platform-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## BOM Version Governance
- BOM is versioned and published to Artifactory on every change
- Services pin a specific BOM version — no floating `LATEST`
- BOM upgrade = deliberate PR in each service repo
- platform-common version is managed inside the BOM

## Key Shared Classes (platform-common)
- `TenantContext` — ThreadLocal: tenantId, tier(ENTERPRISE/PROFESSIONAL/STARTER), schema, kafkaPrefix
- `TenantResolutionFilter` — reads X-Tenant-ID header → validates → sets TenantContext + MDC
- `TenantRoutingDataSource extends AbstractRoutingDataSource` — routes to tenant schema
- `TenantAwareKafkaProducer` — prefixes topic with TenantContext.getKafkaPrefix()
- `StandardResponseEnvelope<T>` — `{ data, meta: { requestId, traceId, timestamp } }`
- `CursorPage<T>` — `{ data, pagination: { cursor, hasMore, pageSize } }`

## Multi-Tenancy: Bridge Model
| Tier         | K8s NS    | Flowable Engine | DB Schema          | Kafka           |
|--------------|-----------|-----------------|--------------------|-----------------|
| ENTERPRISE   | Dedicated | Dedicated pod   | Dedicated schema   | Dedicated prefix|
| PROFESSIONAL | Dedicated | Shared pod      | Dedicated schema   | Dedicated prefix|
| STARTER      | Shared    | Shared pod      | Shared + RLS       | Shared + header |

## DB Schema Pattern
```
PostgreSQL Cluster
  platform_meta          ← tenants, tenant_environments, artifact metadata, policies
  {tenantId}_platform    ← act_re_*, act_ru_*, act_hi_*, act_fo_*, act_dmn_*, custom_*
  shared_starter         ← All STARTER tenants (tenant_id_ column + RLS)
```

## Flowable Hot Tables (act_ru_*)
- `act_ru_execution` — active process tokens (is_concurrent_ for parallel branches)
- `act_ru_task` — active human tasks (assignee_, due_date_, form_key_, claim_time_)
- `act_ru_variable` — process variables (Claim Check: store ref, payload in Data Service)
- `act_ru_job` — acquired via FOR UPDATE SKIP LOCKED
- `act_ru_deadletter_job` — exhausted retries, manual replay required

## Kafka Topic Naming
- `{tenantPrefix}.process.events` — process lifecycle events
- `{tenantPrefix}.task.events` — task lifecycle events
- `{tenantPrefix}.audit.events` — all audit events → audit-service
- `{tenantPrefix}.notifications` — notification dispatch
- `{tenantPrefix}.deadletter` — failed integration calls

## Key Patterns
- **Claim Check**: variables > 10KB stored in Data Service, ref ID stored in Flowable
- **Burst Absorption**: process starts → Kafka → ProcessStartConsumer (max.poll.records=20)
- **Redlock**: task claim lock key = `{tenantId}:task-lock:{taskId}` TTL 5s
- **Outbox**: service writes + event publish in same TX via Kafka Transactional Producer
- **Expand-Contract**: never DROP column in one migration — ADD → dual-write → DROP cycle

## Port Map (local dev)
- Kong: 8000 | Keycloak: 8080 | Vault: 8200
- Kafka: 9092 | Schema Registry: 8081 | Kafka UI: 8082
- PostgreSQL: 5432 | Redis: 6379 | ClickHouse: 8123
- Jaeger: 16686 | Grafana: 3000 | Prometheus: 9090
