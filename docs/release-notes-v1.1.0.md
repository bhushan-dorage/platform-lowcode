# Platform v1.1.0 Release Notes

**Release Date**: 2026-07-02  
**Git Tag**: v1.1.0

## What's New

### Connector SPI (platform-integration-service)

The hardcoded connector dispatch in `CamelRouteEngine` has been replaced with a `ConnectorProvider` SPI, making every connector independently pluggable and testable.

- **`ConnectorProvider` interface** — each connector implements `connectorType()`, `sourceUri()`, `intermediateUris()`, and `targetUri()`; Spring auto-discovers all `@Component` implementations
- **Five production-ready providers**: HTTP (scheduler + fetch), JDBC (SQL polling), SFTP (camel-ftp), Email (IMAP/SMTP), Slack (outbound webhook)
- SFTP and Slack connectors are now fully implemented, removing the "SFTP and Slack stubbed" limitation carried from v1.0.0
- `CamelRouteEngine` now builds real Apache Camel routes from `RouteConfig` JSON, with `onException` handlers configured for 3 retries and a 5-second back-off on every route
- `ConnectorProviderRegistry` fails at startup if any registered `ConnectorType` has no provider, preventing silent misconfiguration

See [ADR-0009](adr/0009-connector-spi.md) for the design rationale.

### Metadata-Driven Page Builder

A new page composition layer allows entire application screens to be defined as JSON, with no custom React code required per tenant.

- **`platform-page-service`** — new standalone Spring Boot service on port 8085; REST API at `/api/v1/pages` with cursor-based pagination; stores `PageDefinition` entities with a JSONB `schema` column; Flyway-managed schema migrations; multi-tenant via `TenantContext`
- **Five widget types**: `kpi`, `table`, `form`, `chart`, `text`
- **`PageRenderer`** React component in the portal frontend interprets page schemas into a CSS grid layout, delegating to per-widget components
- New portal route `/pages/:pageKey` renders any page definition by key
- **`FormWidget`** reuses `DynamicFormRenderer` and existing form definitions from `platform-form-service` — no duplication of form logic

See [ADR-0010](adr/0010-metadata-driven-page-builder.md) for the design rationale.

## Bug Fixes

None.

## Known Limitations

- Drag-and-drop visual page editor is not yet available; page schemas must be authored as JSON
- `ChartWidget` supports bar charts only; line charts, pie charts, and sparklines are not yet implemented
- AI-assisted app generation is planned for v1.2.0 and is not included in this release

## Upgrade from v1.0.0

**Database migration** — `platform-page-service` introduces new tables. Before starting the service, run the Flyway migration for each tenant's PostgreSQL schema:

```
V1__create_page_tables.sql
```

This migration is idempotent and can be applied with Flyway's standard `migrate` command. No changes are required to existing tenant schemas managed by other services.

There are no breaking changes to existing service APIs, Kafka topics, or SDK interfaces.

## Performance

Performance targets are unchanged from v1.0.0:

- 10,000 concurrent process starts per Enterprise tenant
- p99 form submission < 2s
- p99 task inbox < 500ms
- Zero tolerance for TASK_ALREADY_CLAIMED conflicts

No regressions are expected from the changes in this release. The Connector SPI refactor is internal to `platform-integration-service` and does not alter its external API surface. `platform-page-service` is a new service with no cross-service synchronous calls on the critical path.
