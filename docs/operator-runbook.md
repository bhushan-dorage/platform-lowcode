# Platform Operator Runbook v1.1.0

## Architecture Overview

The platform consists of 11 microservices behind a Kong API gateway:

| Service | Port | Technology | Depends On |
|---------|------|-----------|-----------|
| platform-workflow-engine | 8080 | Spring Boot, Flowable | PostgreSQL, Redis, Kafka |
| platform-form-service | 8080 | Spring Boot | PostgreSQL, Kafka |
| platform-page-service | 8080 | Spring Boot | PostgreSQL |
| platform-data-service | 8080 | Spring Boot | PostgreSQL, Kafka |
| platform-entitlements-service | 8080 | Spring Boot | PostgreSQL |
| platform-audit-service | 8080 | Spring Boot | ClickHouse, Kafka |
| platform-integration-service | 8080 | Spring Boot, Camel 4 | PostgreSQL |
| platform-notification-service | 8080 | Spring Boot | PostgreSQL, Kafka, SMTP |
| platform-webhook-service | 8080 | Spring Boot | PostgreSQL, Kafka |
| platform-portal-frontend | 80 | React/nginx | Kong |
| kong | 8000/8001 | Kong 3.6 | – |

## Health Checks

```bash
# Check all services via Kong
curl http://localhost:8000/actuator/health

# Check each service directly
for port in 8080 8086 8087 8088 8089; do
  curl -sf http://localhost:$port/actuator/health | jq -r '.status'
done
```

## Backup and Restore

### PostgreSQL

```bash
# Backup
pg_dump -h localhost -U platform -d platform > backup-$(date +%Y%m%d).sql

# Restore
psql -h localhost -U platform -d platform < backup-YYYYMMDD.sql
```

### ClickHouse (audit data)

```bash
# Backup audit_events table
clickhouse-client --query="SELECT * FROM platform_audit.audit_events FORMAT Native" \
  > audit-backup-$(date +%Y%m%d).native

# Restore
clickhouse-client --query="INSERT INTO platform_audit.audit_events FORMAT Native" \
  < audit-backup-YYYYMMDD.native
```

## Scaling

### Kubernetes HPA

HPA manifests are in `infra/k8s/hpa/`. Apply with:

```bash
kubectl apply -f infra/k8s/hpa/
```

Manual scaling:

```bash
kubectl scale deployment platform-workflow-engine -n platform --replicas=5
```

## Incident Response

### High Kafka Consumer Lag

1. Check lag: `kafka-consumer-groups.sh --bootstrap-server kafka:9092 --describe --group audit-service`
2. Scale the consuming service: `kubectl scale deployment platform-audit-service -n platform --replicas=4`
3. Monitor lag decrease with the Grafana Kafka dashboard

### Database Connection Pool Exhausted

1. Check pg_stat_activity: `SELECT count(*), state FROM pg_stat_activity GROUP BY state;`
2. Kill idle connections: `SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < now() - interval '5 minutes';`
3. Reduce HikariCP `maximumPoolSize` if needed and restart pods

### Workflow Engine Task Lock Stuck

If a task is stuck with an unreleased distributed lock:

```bash
# Find and delete the stuck lock in Redis
redis-cli KEYS "platform:task:lock:*"
redis-cli DEL "platform:task:lock:<task-id>"
```

## Log Aggregation

All services emit structured JSON logs (logstash format). Collect with:

```bash
# Follow workflow engine logs
kubectl logs -n platform -l app=platform-workflow-engine -f | jq '.'
```

Key log fields: `level`, `message`, `tenantId`, `traceId`, `requestId`, `exception`.

## Monitoring Dashboards (Grafana)

| Dashboard | URL | Purpose |
|-----------|-----|---------|
| Platform Overview | /d/platform-overview | Request rates, error rates, latency |
| Kafka Lag | /d/kafka-lag | Consumer group lag per service |
| JVM Metrics | /d/jvm | Heap, GC, threads per service |
| Audit Chain | /d/audit-chain | Hash chain integrity status |
