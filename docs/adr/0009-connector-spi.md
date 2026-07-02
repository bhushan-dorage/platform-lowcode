# ADR-0009: Connector SPI for Integration Service

**Status:** Accepted  
**Date:** 2026-07-02

## Context

The integration service needs to support multiple external system connectors (HTTP, JDBC, SFTP, Email, Slack) with the ability to add new connector types without modifying core engine code.

The initial design used a hardcoded `switch` statement in `CamelRouteEngine` keyed on `ConnectorType` enum values. Each connector's URI-building logic lived inline in the engine, making it impossible to:
- Add a new connector type without touching the engine
- Unit-test individual connectors in isolation
- Let third parties contribute connectors

### Candidates

| Option | Description | Trade-off |
|--------|-------------|-----------|
| Enum dispatch (status quo) | `switch (connectorType)` in engine | Simple but closed to extension |
| Strategy map (static) | `Map<ConnectorType, ConnectorStrategy>` wired manually | Testable but still hardcoded |
| SPI with Spring discovery | Interface + `@Component` + `List<ConnectorProvider>` injection | Open/closed; auto-discovered |
| OSGi plugin registry | Full dynamic class-loading | Far too heavy for this service |

## Decision

Adopt a **Connector SPI** (`ConnectorProvider` interface) with Spring-based auto-discovery:

- `ConnectorProvider` interface exposes `connectorType()`, `definition()`, `sourceUri()`, `intermediateUris()`, and `targetUri()`
- Each connector is a `@Component` implementing the interface; Spring injects them as `List<ConnectorProvider>` into `ConnectorProviderRegistry`
- `ConnectorProviderRegistry` builds an immutable `Map<ConnectorType, ConnectorProvider>` at startup — fail-fast if any type is unmapped
- `CamelRouteEngine` injects the registry and asks each provider for its URIs; it no longer knows anything about connector-specific logic
- `ConnectorCatalog` (REST API) also delegates to the registry, eliminating its own hardcoded list

The `intermediateUris()` default method (returns `List.of()`) handles connectors like HTTP where Camel needs a scheduler source + an intermediate HTTP fetch before reaching the target.

## Consequences

**Positive**
- Adding a connector = one new `@Component` class; no engine changes required (Open/Closed Principle)
- Each provider is independently unit-testable against a plain `ConnectorCatalogTest` — no Spring context needed
- The REST catalog and the route engine share a single source of truth via the registry
- `ConnectorProviderRegistry` fails at startup if a registered `ConnectorType` has no provider, catching mismatches early

**Negative**
- `ConnectorType` enum still needs an entry for each connector; it can't be fully open without a string-keyed registry
- Providers that need credentials (SFTP password, SMTP auth) embed them in the Camel URI; a future secrets-manager integration will need a `SecretResolver` hook on the interface
- `intermediateUris()` is a leaky abstraction — it exposes Camel URI details at the SPI boundary; a richer `RouteContributor` API may be needed if non-Camel engines are added
