# Platform v1.0.0 Release Notes

**Release Date**: 2026-06-25  
**Git Tag**: v1.0.0

## What's Included

### Core Services (Phases 1-4)
- **platform-workflow-engine**: Flowable BPMN process execution, distributed task locking, SLA monitoring
- **platform-form-service**: JSON Schema form management, validation, versioning
- **platform-data-service**: Multi-tenant entity modeling and CRUD
- **platform-entitlements-service**: RBAC + ABAC policy engine
- **platform-studio-backend**: Git-backed artifact store, process/form deployment

### Operational Services (Phases 5-6)
- **platform-audit-service**: SHA-256 hash chain audit trail, SIEM export (CEF/LEEF/JSON), compliance reports (SOC2/ISO27001/GDPR/HIPAA)
- **platform-integration-service**: Apache Camel dynamic routes, 5-connector catalog
- **platform-notification-service**: Multi-channel (email/SMS/push/in-app), Kafka-driven
- **platform-webhook-service**: HMAC-SHA256 signed outbound webhooks, 5-attempt retry

### Consumer SDKs (Phase 7)
- **platform-portal-frontend**: React 18 SPA with Keycloak PKCE, real-time task inbox, dynamic form rendering
- **platform-sdk-java**: Maven multi-module SDK (core, process, task, Spring Boot starter)
- **platform-sdk-js**: TypeScript npm package with entity code generator

### Infrastructure (Phase 8)
- Gatling 3.9 load test suite (4 scenarios, 10k concurrent process starts)
- Kubernetes HPA configs for all services
- Chaos engineering test scripts (pod kill, stale lock recovery)
- OpenAPI 3.1 complete API specification
- Operator runbook and tenant onboarding guide

## Performance Targets
- 10,000 concurrent process starts per Enterprise tenant
- p99 form submission < 2s
- p99 task inbox < 500ms
- Zero tolerance for TASK_ALREADY_CLAIMED conflicts

## Breaking Changes
None — this is the first production release.

## Known Limitations
- SFTP and Slack connectors in integration-service are stubbed (HTTP and JDBC are production-ready)
- SMS and push channels in notification-service are stubbed (email and in-app are production-ready)
- Studio frontend UI requires separate deployment from this release bundle

## Upgrade Path
N/A — first release.

## Security
- All SQL uses parameterized queries (SQLi prevention)
- JWT Bearer authentication on all endpoints
- HMAC-SHA256 webhook signature verification
- SHA-256 audit event hash chain (tamper detection)
- TLS enforced in production via Kong
