#!/usr/bin/env bash
# Chaos Test: Stale distributed lock recovery
# Simulates a lock holder dying mid-execution and verifies another node picks it up
set -euo pipefail

NAMESPACE="${NAMESPACE:-platform}"
REDIS_POD="${REDIS_POD:-redis-0}"
LOCK_KEY_PREFIX="${LOCK_KEY_PREFIX:-platform:task:lock:}"
TASK_ID="${TASK_ID:-}"

echo "=== Chaos Test: Stale Lock Recovery ==="

if [ -z "$TASK_ID" ]; then
  echo "Fetching an open task to use..."
  TASK_ID=$(curl -sf "$PLATFORM_BASE_URL/api/v1/tasks?status=OPEN&pageSize=1" \
    -H "Authorization: Bearer $PLATFORM_TOKEN" | jq -r '.content[0].id')
fi
echo "Task ID: $TASK_ID"

# 1. Manually set an expired lock in Redis (TTL=0 means stale)
echo "[1/4] Injecting stale lock into Redis..."
kubectl exec -n "$NAMESPACE" "$REDIS_POD" -- \
  redis-cli SET "${LOCK_KEY_PREFIX}${TASK_ID}" "dead-node-lock-holder" EX 1
echo "  Stale lock set (expires in 1s)"
sleep 2

# 2. Try to claim the task — should succeed because lock expired
echo "[2/4] Claiming task (lock should have expired)..."
HTTP_CODE=$(curl -sf -o /dev/null -w "%{http_code}" \
  -X POST "$PLATFORM_BASE_URL/api/v1/tasks/$TASK_ID/claim" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" -d '{}')

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
  echo "  PASS: Task claimed successfully (HTTP $HTTP_CODE)"
else
  echo "  FAIL: Expected 200/201, got HTTP $HTTP_CODE"
  exit 1
fi

# 3. Verify no duplicate execution by checking audit log
echo "[3/4] Verifying single execution in audit log..."
CLAIM_COUNT=$(curl -sf "$PLATFORM_BASE_URL/api/v1/audit?resourceId=$TASK_ID&operation=TASK_CLAIM" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" | jq '.totalElements')
if [ "${CLAIM_COUNT:-0}" -eq 1 ]; then
  echo "  PASS: Exactly 1 claim event in audit log"
else
  echo "  WARN: Expected 1 claim event, found $CLAIM_COUNT"
fi

echo "[4/4] Chaos test complete."
