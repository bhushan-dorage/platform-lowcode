# ADR-0007: Kong as the API Gateway

**Status:** Accepted  
**Date:** 2026-01-15

## Context

All external traffic must pass through a single gateway that handles:

- JWT validation (Keycloak tokens) — services should not each implement token validation
- Rate limiting — protect against abuse without application-level code
- Request correlation — attach `X-Request-ID` to every request for distributed tracing
- Request size enforcement — prevent oversized payloads reaching services
- TLS termination in production
- Path-based routing to downstream services
- DB-less declarative configuration — no additional database for the gateway itself

Candidates evaluated:

| Gateway | Notes |
|---|---|
| **Kong** | OSS, declarative DB-less mode, Lua plugin ecosystem, Kubernetes Ingress Controller available |
| AWS API Gateway | Managed, but vendor lock-in; limited flexibility for on-premises deployment |
| NGINX | Highly performant, but routing and plugin configuration requires Lua scripting; no built-in rate-limiting management UI |
| Traefik | Docker/Kubernetes-native, dynamic config, but plugin ecosystem smaller than Kong |
| Spring Cloud Gateway | JVM-based, easy to customise in Java, but adds another Spring Boot service to operate |
| Envoy | Extremely powerful, but complex configuration (xDS API); better suited as a service mesh sidecar |

## Decision

Use **Kong 3.6** in **DB-less declarative mode** configured via `infra/kong/kong.yaml`.

Active plugins (applied globally):

| Plugin | Configuration |
|---|---|
| `rate-limiting` | 1,000 req/min, 10,000 req/hr per consumer; `policy: local` (per-pod counters) |
| `correlation-id` | Generates UUID `X-Request-ID` on every request; echoed downstream |
| `request-size-limiting` | 10 MB max payload size |

JWT validation is handled by Spring Security in each service (not at the Kong layer) so that the full Keycloak JWT claim set is available within service business logic for tenant resolution and entitlements.

For Kubernetes production deployment, the **Kong Kubernetes Ingress Controller** replaces the Docker-based setup, reading route configuration from `KongIngress` CRDs.

## Consequences

**Positive:**
- DB-less mode eliminates the Kong database dependency — gateway configuration is version-controlled YAML
- Single rate-limiting enforcement point — no per-service rate-limit code
- `X-Request-ID` correlation propagated to all downstream services and included in every log line and API response envelope
- Kong's plugin model allows adding auth, caching, or IP restriction without touching application code
- Kong Ingress Controller provides a smooth path from Docker Compose to Kubernetes

**Negative:**
- `policy: local` rate-limiting counts per-pod, not cluster-wide — at 10 pods, the effective limit is 10× the configured value. Requires Redis-backed `policy: redis` for accurate cluster-wide limiting in production
- JWT validation at the application layer (not Kong) means an invalid token still reaches the service before being rejected — one extra network hop vs. gateway-level rejection
- DB-less mode limits dynamic plugin configuration — adding a new route requires a config reload (`kong reload`) or pod restart
- Kong's Lua plugin API requires Lua knowledge for custom plugin development; most teams prefer Java/TypeScript
- Rate-limit counters are in-memory per pod and lost on restart — spikes can briefly exceed limits during rolling deploys
