# Entitlements Engine — Three-Dimensional Access Control

## Dimension 1: Functional (RBAC)
Permission pattern: `platform:{resource}:{action}`
```
platform:process:start
platform:task:claim
platform:task:complete
platform:report:export
platform:studio:process:edit
```
- Roles aggregate permissions
- Child roles inherit parent
- Conflict resolution: ALLOW wins over DENY

## Dimension 2: Data Level (ABAC)
YAML policy → compiled SQL predicate (done at activation time, not runtime)
```yaml
# Stored in platform_meta.data_policies
role: L1_APPROVER
entity: LoanApplication
operation: READ
conditions:
  - field: branchId
    op: EQUALS
    value: "${actor.attributes.branchId}"
  - field: loanAmount
    op: LESS_THAN_OR_EQUAL
    value: "${actor.attributes.sanctioningLimit}"
  - field: status
    op: NOT_IN
    values: ["DRAFT", "SYSTEM_HOLD"]
```

Compiled to parameterized SQL:
```sql
branch_id = :actorBranchId AND loan_amount <= :actorSanctioningLimit
AND status NOT IN ('DRAFT','SYSTEM_HOLD')
```

Enforcement layers (all three required):
1. Predicate injected into list queries (WHERE clause)
2. Post-fetch assertion for single record GET
3. PostgreSQL RLS for STARTER tier (shared schema defense-in-depth)

## Dimension 3: Field Level
| Access Level | Read Behavior              | Write Behavior         |
|-------------|----------------------------|------------------------|
| ALLOW       | Field in response          | Field accepted         |
| DENY        | Field omitted entirely     | 403 Forbidden          |
| MASKED      | Value masked (PAN****1234) | Always DENY            |
| CONDITIONAL | Depends on runtime expr    | Allowed if expr=true   |

Mask patterns:
- PAN: first 4 + `****` + last 4 → `ABCD****1234`
- Email: first char + `*****` + `@domain`
- Mobile: `****` + last 4

## Cache Strategy
```
L1 Caffeine (in-process):
  effective-permissions:{userId} → TTL 5min
  field-policy:{tenantId}:{roleId}:{entityType} → TTL 15min
  data-policy-compiled:{tenantId}:{roleId}:{entityType} → TTL 15min

L2 Redis (tenant-scoped):
  {tenantId}:role-perms:{roleId} → TTL 30min
  {tenantId}:sql-predicate:{roleId}:{entityType} → TTL 60min
```

Invalidation: Any role/policy change → immediate flush of affected keys via `@CacheEvict`.

## AOP Enforcement Points
```java
@RequiresPermission("platform:task:claim")
public ClaimedTask claimTask(String taskId) { ... }

// Data level: injected via DataLevelQueryEnforcer
List<LoanApplication> apps = entityClient.list(LoanApplication.class); // predicate auto-injected

// Field level: applied in response filter
@FieldLevelFiltered(entityType = "LoanApplication")
public LoanApplication getById(String id) { ... }
```

## ACCESS_DENIED Audit Event
Every access denial must publish to audit:
```json
{
  "eventType": "ACCESS_DENIED",
  "domain": "ACCESS",
  "actor": { "userId": "...", "roles": [...] },
  "resource": { "type": "LoanApplication", "id": "..." },
  "action": { "operation": "READ", "denialReason": "DATA_POLICY_VIOLATION" }
}
```
