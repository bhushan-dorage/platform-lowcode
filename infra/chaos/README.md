# Chaos Engineering Tests

## Prerequisites

- `kubectl` configured and pointing to the target cluster
- `curl` and `jq` installed
- Environment variables set:
  - `PLATFORM_BASE_URL` — e.g. `http://kong.platform.svc:8000`
  - `PLATFORM_TOKEN` — valid OAuth2 bearer token
  - `NAMESPACE` — Kubernetes namespace (default: `platform`)

## Test 1: Engine Pod Kill

Verifies that an in-flight process continues to completion after a workflow engine pod is killed.

```bash
export PLATFORM_BASE_URL=http://localhost:8000
export PLATFORM_TOKEN=$(./scripts/get-token.sh)
bash infra/chaos/engine-pod-kill-test.sh
```

**Pass criteria:** Process reaches COMPLETED status within 120 seconds of pod replacement.

## Test 2: Stale Lock Recovery

Verifies that a task locked by a dead node can be claimed by another node after the lock expires.

```bash
export TASK_ID=<task-uuid>
bash infra/chaos/stale-lock-recovery-test.sh
```

**Pass criteria:** Task is claimed with HTTP 200, exactly 1 claim event in audit log.

## Running Against CI

Add to your CI pipeline after deploying to a staging cluster:

```yaml
- name: Chaos Tests
  run: |
    bash infra/chaos/engine-pod-kill-test.sh
    bash infra/chaos/stale-lock-recovery-test.sh
  env:
    PLATFORM_BASE_URL: ${{ secrets.STAGING_BASE_URL }}
    PLATFORM_TOKEN: ${{ secrets.STAGING_TOKEN }}
    NAMESPACE: platform-staging
```
