# ADR-0008: PostgreSQL Schema-per-Tenant Multi-Tenancy Model

**Status:** Accepted  
**Date:** 2026-01-15

## Context

The platform must support multiple isolated tenants with different scale, compliance, and data-residency requirements. Four standard multi-tenancy models were considered:

| Model | Isolation | Cost | Complexity |
|---|---|---|---|
| Shared database, shared schema | Lowest | Lowest | Low — row-level filtering only |
| Shared database, schema per tenant | Medium | Low | Medium — schema routing |
| Database per tenant | Highest | High | High — connection pool per tenant |
| Hybrid (tier-based) | Tier-matched | Tier-matched | High — multiple models in one system |

Requirements by tier:

- **Enterprise**: data isolation required for compliance (SOC2, HIPAA); dedicated Flowable engine; peak 10,000 concurrent process starts
- **Professional**: moderate isolation; shared infrastructure acceptable; moderate throughput
- **Starter**: lowest cost; full resource sharing acceptable; low throughput

## Decision

Use a **hybrid schema model** driven by `TenantContext.getTier()`:

| Tier | Database | Schema | Flowable Engine |
|---|---|---|---|
| ENTERPRISE | `platform` | `{tenantId}_platform` (dedicated) | Dedicated `ProcessEngine` bean |
| PROFESSIONAL | `platform` | `{tenantId}_platform` (dedicated) | Shared engine with Flowable `tenantId` column |
| STARTER | `platform` | `shared_starter` | Shared engine with Flowable `tenantId` column |

`TenantRoutingDataSource` (extends `AbstractRoutingDataSource`) switches the active schema based on `TenantContext`. Flyway migrations run per-schema at tenant provisioning time via `TenantSchemaManager`.

STARTER tenants in `shared_starter` are additionally protected by PostgreSQL Row-Level Security (RLS) policies that enforce `tenant_id = current_setting('app.tenant_id')`.

## Consequences

**Positive:**
- Enterprise tenants get full schema isolation — a SQL bug in one tenant cannot read another's data
- Shared schema for Starter keeps infrastructure cost low for the long tail of small tenants
- `TenantRoutingDataSource` is transparent to all repositories — no tenant-aware query code needed in services
- Schema-per-tenant makes point-in-time restore and tenant data export straightforward (`pg_dump -n {tenantId}_platform`)
- PostgreSQL RLS as a defence-in-depth layer for Starter tenants prevents accidental cross-tenant data leaks

**Negative:**
- At high Enterprise tenant counts, PostgreSQL connection pool size grows proportionally (one pool per tenant schema) — must be managed with PgBouncer or connection pool sizing limits
- Flyway migration of all tenant schemas takes time at service startup — mitigated by running migrations asynchronously at tenant onboarding, not at pod startup
- `shared_starter` RLS policies must be applied to every table — a missed policy is a data leak; requires test coverage
- `AbstractRoutingDataSource` does not support XA (distributed) transactions across schemas — cross-tenant operations (e.g., platform admin reads) require a separate admin data source
- The hybrid model has three code paths to test and maintain; a pure model would be simpler but fails to meet Enterprise compliance or Starter cost requirements simultaneously
