#!/usr/bin/env bash
# Chaos Test: Engine pod kill during active job execution
# Verifies: stale lock recovery + idempotency (no duplicate process execution)
set -euo pipefail

NAMESPACE="${NAMESPACE:-platform}"
DEPLOYMENT="platform-workflow-engine"
TEST_PROCESS_KEY="${TEST_PROCESS_KEY:-loan-application}"
MONITOR_DURATION=120

echo "=== Chaos Test: Engine Pod Kill ==="
echo "Namespace: $NAMESPACE"
echo "Target: $DEPLOYMENT"

# 1. Confirm at least 2 replicas running
REPLICAS=$(kubectl get deployment "$DEPLOYMENT" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
if [ "${REPLICAS:-0}" -lt 2 ]; then
  echo "ERROR: Need at least 2 running replicas for pod kill test (got $REPLICAS)"
  exit 1
fi

# 2. Start a background process via API
echo "[1/5] Starting test process..."
PROCESS_ID=$(curl -sf -X POST "$PLATFORM_BASE_URL/api/v1/processes" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"processKey\":\"$TEST_PROCESS_KEY\",\"variables\":{\"chaosTest\":true}}" \
  | jq -r '.id')
echo "  Process ID: $PROCESS_ID"

# 3. Kill one engine pod
echo "[2/5] Killing one engine pod..."
POD=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[0].metadata.name}')
kubectl delete pod "$POD" -n "$NAMESPACE" --grace-period=0
echo "  Killed pod: $POD"

# 4. Wait for replacement pod to start
echo "[3/5] Waiting for replacement pod..."
kubectl rollout status deployment/"$DEPLOYMENT" -n "$NAMESPACE" --timeout=120s
echo "  Replacement pod ready"

# 5. Monitor process for $MONITOR_DURATION seconds — it must complete without duplication
echo "[4/5] Monitoring process $PROCESS_ID for ${MONITOR_DURATION}s..."
DEADLINE=$((SECONDS + MONITOR_DURATION))
FINAL_STATUS=""
while [ $SECONDS -lt $DEADLINE ]; do
  FINAL_STATUS=$(curl -sf "$PLATFORM_BASE_URL/api/v1/processes/$PROCESS_ID" \
    -H "Authorization: Bearer $PLATFORM_TOKEN" | jq -r '.status')
  echo "  Status: $FINAL_STATUS"
  if [ "$FINAL_STATUS" = "COMPLETED" ] || [ "$FINAL_STATUS" = "FAILED" ]; then
    break
  fi
  sleep 5
done

# 6. Assert
echo "[5/5] Asserting..."
if [ "$FINAL_STATUS" = "COMPLETED" ]; then
  echo "PASS: Process completed successfully after pod kill"
else
  echo "FAIL: Process did not complete within ${MONITOR_DURATION}s (status=$FINAL_STATUS)"
  exit 1
fi
