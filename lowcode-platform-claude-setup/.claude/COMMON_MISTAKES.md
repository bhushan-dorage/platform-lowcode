# Common Mistakes — DO NOT REPEAT

## P0: Tenant Isolation Leaks
- **Missing tenant filter**: Every DB query MUST include `TenantContext.getSchema()` routing
  AND `tenant_id_ = TenantContext.get()` for STARTER tier (shared schema).
  Missing this = data leak across tenants.
- **Kafka topic without prefix**: Always use `TenantContext.getKafkaPrefix()` when producing.
  Never hardcode topic names.
- **TenantContext not cleared**: TenantResolutionFilter must clear in `finally` block.
  Leaked ThreadLocal = wrong tenant data in next request on same thread.

## P0: Flowable Anti-Patterns
- **Direct act_ru_* SQL**: Never query/update Flowable runtime tables directly.
  Always use RuntimeService, TaskService, etc.
- **Offset pagination on task inbox**: act_ru_task grows; offset breaks on concurrent inserts.
  Always cursor paginate (WHERE id > lastSeenId ORDER BY id).
- **Large variables in act_ru_variable**: Variables > 10KB MUST use Claim Check pattern.
  Large payloads cause act_ru_variable bloat and slow engine queries.
- **Optimistic locking for jobs**: Use FOR UPDATE SKIP LOCKED. Optimistic causes retry storms.

## P1: API Design Errors
- **Offset pagination**: Breaks on concurrent inserts. Always cursor-based.
- **Sync process start returning 201**: Must be async 202 + trackingId.
  Process start goes via Kafka for burst absorption.
- **Missing Idempotency-Key on POST mutations**: All state-changing POSTs need idempotency support.
- **Non-standard error envelope**: Always `{ error: { code, message, traceId, details } }`.
  Never raw exception messages.

## P1: Caching Pitfalls
- **Cache without tenant scope**: Redis keys MUST be prefixed with tenantId.
  e.g. `{tenantId}:entitlements:{userId}` not `entitlements:{userId}`.
- **Wrong TTL for entitlements**: L1 Caffeine 5min (permissions), 15min (field policy).
  L2 Redis 30min (role→perms), 60min (compiled SQL predicate).
- **Not invalidating on policy change**: Any role/data/field policy change → flush relevant keys.

## P1: Schema Migration Errors
- **DROP column in one migration**: Use Expand-Contract. ADD → dual-write → DROP in next cycle.
- **Missing tenant_id_ index on shared_starter schema**: Every shared table needs index on tenant_id_.
- **Flyway on wrong schema**: Always configure Flyway datasource to point to tenant schema via TenantContext.

## P2: Observability Gaps
- **Missing traceId propagation**: Every service must extract W3C TraceContext from incoming request
  and propagate to outbound calls and Kafka message headers.
- **No Micrometer timer on service methods**: Every public service method needs
  `Timer.Sample` or `@Timed` annotation.
- **Missing tenantId in MDC**: TenantResolutionFilter must set `MDC.put("tenantId", ...)`.
  Without this, logs can't be filtered per tenant.

## P2: Security
- **JWT claims not validated beyond signature**: Always validate: issuer (realm URL), audience,
  tenant_id claim matches X-Tenant-ID header.
- **Vault path not scoped to tenant**: Dynamic DB creds path = `/database/creds/{tenantId}-role`.
  Never use shared DB role across tenants.
