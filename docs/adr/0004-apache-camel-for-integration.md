# ADR-0004: Apache Camel for the Integration Engine

**Status:** Accepted  
**Date:** 2026-01-15

## Context

The platform needs an integration engine that allows tenants to connect external systems (HTTP APIs, databases, file servers, messaging systems) to platform workflows without writing code. Requirements:

- Runtime-configurable routes — routes are stored in the database and started/stopped via API, not deployed as code
- Wide connector catalogue out of the box (HTTP, JDBC, Kafka, SFTP, Slack, SAP, Salesforce, etc.)
- Embeddable in the Spring Boot service — no separate integration server to operate
- Support for EIP (Enterprise Integration Patterns): content-based routing, splitter, aggregator, dead-letter channel
- Spring Boot native support

Candidates evaluated:

| Engine | Notes |
|---|---|
| **Apache Camel** | 200+ components, Spring Boot starter, dynamic route loading via `CamelContext`, Apache 2.0 |
| MuleSoft Anypoint | Industry-leading, but proprietary and expensive; not embeddable |
| Spring Integration | Annotation/DSL-based, tightly coupled to Spring; harder to make runtime-configurable |
| Boomi | SaaS iPaaS, not embeddable, per-connection pricing |
| WSO2 Micro Integrator | Open-source, but XML-DSL heavy and smaller community |
| Temporal (for orchestration) | Workflow-oriented, not a connector framework |

## Decision

Use **Apache Camel 4.x** embedded in `platform-integration-service` via the `camel-spring-boot-starter`.

`CamelRouteEngine` dynamically adds/removes routes from the live `CamelContext` based on `RouteDefinitionEntity` records loaded from PostgreSQL. Tenants configure routes through the REST API at `/api/v1/integrations`; routes activate immediately without a service restart.

The `ConnectorCatalog` defines the supported connector types (`HTTP`, `JDBC`, `KAFKA`, `SFTP`, `SLACK`). Each `ConnectorDefinition` maps to a Camel component URI.

## Consequences

**Positive:**
- 200+ Camel components available — adding a new connector type is adding a new `ConnectorType` enum value and a URI template
- Dynamic route loading means tenant self-service integration without platform redeployment
- Built-in EIP implementations (dead-letter channel, retry, split/aggregate) reduce custom code
- `camel-spring-boot-starter` integrates with Spring's transaction manager, health indicators, and metrics
- Active Apache community; Camel Quarkus available if a GraalVM native build is needed later

**Negative:**
- `ConnectorType` is currently a Java enum — adding a new connector type requires recompiling the service (see ADR-0007 for the planned SPI migration)
- Dynamic route DSL is built in Java code; non-Java developers cannot contribute new connector implementations without a JVM build toolchain
- Camel's error handling DSL (`onException`, `deadLetterChannel`) is powerful but has a steep learning curve
- Testing dynamic routes requires a full Camel context in tests, which is slower than unit tests
- Camel's XML DSL (legacy) and Java DSL coexist; new code must use the Java DSL only to avoid mixed-style maintenance
