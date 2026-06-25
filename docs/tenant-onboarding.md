# Tenant Onboarding Guide v1.0.0

## Prerequisites

- Platform v1.0.0 deployed and healthy (all services `UP`)
- Keycloak realm `platform` configured
- Tenant tier decided: STARTER, PROFESSIONAL, or ENTERPRISE

## Step 1: Provision Tenant in Keycloak

1. Log in to Keycloak Admin Console
2. Create a new group named `tenant-{tenantId}`
3. Add a `tenant_id` attribute: `{tenantId}`
4. Set `tier` attribute: `ENTERPRISE` / `PROFESSIONAL` / `STARTER`
5. Create client credentials for the tenant's service accounts

## Step 2: Initialize Database Schema

For ENTERPRISE and PROFESSIONAL tenants (dedicated schemas):

```sql
-- Run against PostgreSQL as superuser
CREATE SCHEMA IF NOT EXISTS "{tenantId}_platform";
GRANT ALL ON SCHEMA "{tenantId}_platform" TO platform;
```

STARTER tenants share the `shared_starter` schema (no action needed).

## Step 3: Provision ClickHouse Audit Partition

```sql
-- Run against ClickHouse
ALTER TABLE platform_audit.audit_events
  ADD PARTITION BY tenant_id = '{tenantId}';
```

## Step 4: Seed Initial Roles and Policies

```bash
# Using the Platform API
TOKEN=$(curl -s .../token | jq -r .access_token)

# Create admin role
curl -X POST http://localhost:8000/api/v1/entitlements/roles \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"ADMIN","permissions":["*"],"tenantId":"{tenantId}"}'

# Create read-only role
curl -X POST http://localhost:8000/api/v1/entitlements/roles \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"READER","permissions":["READ"],"tenantId":"{tenantId}"}'
```

## Step 5: Configure Webhook for Tenant Events (Optional)

```bash
curl -X POST http://localhost:8000/api/v1/webhooks \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "url": "https://tenant-system.example.com/platform-events",
    "secret": "$(openssl rand -hex 32)",
    "eventTypes": ["TASK_COMPLETED","PROCESS_COMPLETED","FORM_SUBMITTED"]
  }'
```

## Step 6: Smoke Test

```bash
# Start a test process
curl -X POST http://localhost:8000/api/v1/processes \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"processKey":"smoke-test","variables":{"tenantId":"{tenantId}"}}'
```

Expected: HTTP 201 with a process ID.

## Onboarding Checklist

- [ ] Keycloak group and client created
- [ ] Database schema provisioned (ENTERPRISE/PROFESSIONAL)
- [ ] ClickHouse partition created
- [ ] Admin and Reader roles seeded
- [ ] Webhook configured (optional)
- [ ] Smoke test process started successfully
- [ ] Tenant notified with API credentials and portal URL
