# ADR-0010: Metadata-Driven Page Builder

**Status:** Accepted  
**Date:** 2026-07-02

## Context

The platform has `platform-form-service` (JSON Schema form definitions) and `DynamicFormRenderer` (rjsf-based renderer) but no concept of a *page* — a composition of multiple widgets (KPI cards, tables, charts, forms, text blocks) arranged in a grid. Without this layer:

- Every application screen requires a custom React component authored and deployed per tenant
- Tenant-specific UI customisation triggers a code deployment, violating the platform's no-code/low-code promise
- There is no auditable or versionable record of what a screen looks like; layout changes are buried in frontend commits

### Candidates

| Option | Description | Trade-off |
|--------|-------------|-----------|
| Custom React components per tenant (status quo) | Each screen is a hand-authored component shipped in a tenant-specific bundle | Fast for one screen; does not scale; every change requires a code deployment |
| Embed-only (iframes to micro-frontends) | Tenant teams build separate SPAs; portal surfaces them in iframes | Maximum flexibility; requires a full frontend infrastructure per tenant; no shared component library or auth propagation |
| Metadata-driven page schema (`platform-page-service` + `PageRenderer`) | Page definitions stored as JSONB in a new service; frontend interprets schema at runtime | New screens deploy as JSON; reuses existing form infrastructure; page layout is versionable and auditable |

## Decision

Adopt a **metadata-driven page builder** consisting of a new backend service and a frontend renderer:

**`platform-page-service`** — standalone Spring Boot service (port 8085):
- Stores `PageDefinition` entities with a JSONB `schema` column in PostgreSQL (schema-per-tenant via `TenantContext`)
- REST API at `/api/v1/pages` with cursor-based pagination
- Flyway manages schema migrations (`V1__create_page_tables.sql`)
- Multi-tenant isolation enforced through the shared `TenantContext` propagated from the Kong JWT claim

**Page schema format:**
```json
{
  "version": 1,
  "title": "...",
  "description": "...",
  "layout": {
    "type": "sections",
    "sections": [
      {
        "id": "...",
        "title": "...",
        "columns": 2,
        "widgets": [ ... ]
      }
    ]
  }
}
```

**Five widget types:** `kpi`, `table`, `form`, `chart`, `text`

**`PageRenderer` React component** (portal frontend):
- Fetches the `PageDefinition` from `platform-page-service` by `pageKey`
- Interprets the `layout.sections` into a CSS grid, respecting each section's `columns` value
- Delegates to a per-widget component: `KpiWidget`, `TableWidget`, `FormWidget`, `ChartWidget`, `TextWidget`
- `FormWidget` reuses the existing `DynamicFormRenderer` backed by `platform-form-service` — no duplication of form logic
- `ChartWidget` renders bar charts using pure SVG (no third-party charting library)
- Route `/pages/:pageKey` added to the portal router

## Consequences

**Positive**
- New screens deploy as JSON configuration; no React code changes or frontend redeployments are required
- Form and page concerns are separated: `platform-form-service` owns field-level schema; `platform-page-service` owns layout and composition
- Page schemas are stored in the database and are therefore versionable, auditable, and inspectable without reading source code
- `ChartWidget` has no library dependency for the basic bar-chart case, keeping the bundle lightweight
- `PageRenderer` is compositional: adding a new widget type only requires a new renderer component wired to the switch in `PageRenderer`

**Negative**
- The widget type enum must be extended in both the backend schema and the frontend renderer for every new widget type; there is no fully open plugin mechanism yet
- No drag-and-drop visual editor exists at runtime; page schemas must be authored as JSON (by developers or a future visual tool)
- `ChartWidget` is limited to bar charts; line charts, pie charts, and sparklines require either a dedicated charting library or significant additional SVG logic
- No server-side rendering; pages are client-rendered, making them unsuitable for SEO-sensitive public content
