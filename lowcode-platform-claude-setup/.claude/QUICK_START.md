# Quick Start — Daily Commands

## Local Dev Stack
```bash
# Start full stack
docker-compose up -d

# Start specific services
docker-compose up -d postgres redis kafka keycloak vault

# View logs
docker-compose logs -f platform-workflow-engine

# Reset all data (wipe volumes)
docker-compose down -v && docker-compose up -d
```

## Build & Test
```bash
# Build all modules
mvn clean install -DskipTests

# Build single module
mvn clean install -pl platform-workflow-engine -am -DskipTests

# Run unit tests
mvn test -pl platform-workflow-engine

# Run integration tests (needs Docker)
mvn verify -pl platform-workflow-engine -Pintegration-test

# Run specific test
mvn test -pl platform-workflow-engine -Dtest=TenantRoutingTest
```

## Database
```bash
# Flyway migrate (platform_meta)
mvn flyway:migrate -pl db -Pflyway-meta

# Flyway migrate (tenant schema)
mvn flyway:migrate -pl db -Pflyway-tenant -Dtenant.schema=hsbc_platform

# Connect to PostgreSQL
psql postgresql://localhost:5432/platform -U platform_admin

# Switch to tenant schema
SET search_path TO hsbc_platform;

# Check Flowable runtime jobs
SELECT * FROM act_ru_job ORDER BY create_time_ DESC LIMIT 20;
SELECT * FROM act_ru_deadletter_job ORDER BY create_time_ DESC LIMIT 10;
```

## Kafka
```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Consume audit events
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic hsbc.audit.events --from-beginning

# Check consumer lag
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group audit-service-group
```

## Keycloak
```bash
# Get token (Portal user)
curl -X POST http://localhost:8080/realms/hsbc/protocol/openid-connect/token \
  -d 'grant_type=password&client_id=portal&username=testuser&password=test123'

# Get service token (API client)
curl -X POST http://localhost:8080/realms/hsbc/protocol/openid-connect/token \
  -d 'grant_type=client_credentials&client_id=api-client&client_secret=secret'
```

## Claude Code Workflow (token-efficient)
```bash
# Start session — check context burn
/context

# Compact when approaching limit (not at limit)
/compact

# Use Sonnet for most tasks
/model sonnet

# Switch to Opus only for: arch decisions, complex multi-class design
/model opus

# Check cost after heavy session
/cost

# Clear between unrelated tasks
/clear
```

## Common API Calls (local)
```bash
# Start process (async)
curl -X POST http://localhost:8000/api/v1/processes \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: hsbc" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"processKey":"LoanApproval","businessKey":"LOAN-001","variables":{}}'

# Poll tracking status
curl http://localhost:8000/api/v1/processes/$TRACKING_ID/status \
  -H "Authorization: Bearer $TOKEN" -H "X-Tenant-ID: hsbc"

# Task inbox
curl http://localhost:8000/api/v1/tasks/inbox \
  -H "Authorization: Bearer $TOKEN" -H "X-Tenant-ID: hsbc"
```
