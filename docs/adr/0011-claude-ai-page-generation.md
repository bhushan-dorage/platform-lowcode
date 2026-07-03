# ADR-0011: Claude API for AI-Assisted Page Generation

**Status:** Accepted  
**Date:** 2026-07-03

## Context

ADR-0010 introduced a metadata-driven page builder where screens are described as JSON (`PageSchema`) and rendered by `PageRenderer` at runtime — eliminating the need to deploy custom React components per tenant. However, authoring a `PageSchema` still requires a developer to hand-write JSON: knowing the widget types, config shapes, data-source URLs, and layout grid options.

For the platform's low-code promise to extend to non-technical users, page creation should be achievable through natural language — "Give me an order management dashboard with KPIs, a recent orders table, and a bar chart of orders by status" — and have the system produce a deployable `PageSchema` automatically.

### Options considered

| Option | Description | Trade-off |
|--------|-------------|-----------|
| Rule-based template selection | Map keyword patterns in the prompt to a library of static templates | Fast; no API dependency; limited to a fixed template set; cannot handle novel combinations or domain-specific requirements |
| Local LLM (e.g. Ollama) | Deploy a self-hosted language model in the cluster | Full control over data residency; requires GPU infrastructure; significantly more ops overhead; model quality lags hosted frontier models |
| OpenAI GPT-4 / Azure OpenAI | Call OpenAI API with a schema-describing system prompt | Commercially viable; strong code/JSON generation; requires vendor contract; no Anthropic-native alignment guarantees |
| **Anthropic Claude API** (`claude-opus-4-8`) | Call Anthropic API with a detailed system prompt describing the `PageSchema` format | State-of-the-art instruction following for structured JSON output; first-party Anthropic SDK (`anthropic-java`); extended context window (1M tokens); consistent format compliance with minimal post-processing |

## Decision

Use the **Anthropic Claude API** (`claude-opus-4-8`) for AI-assisted page generation, surfaced as a new endpoint on `platform-page-service` and a `PageGenerator` UI component in the portal.

### Backend — `POST /api/v1/pages/generate`

A new endpoint in `PageController` accepts a `GeneratePageRequest` (`prompt: String`) and delegates to `PageGenerationService`:

1. The service builds a `MessageCreateParams` with a detailed **system prompt** that specifies:
   - The exact `PageSchema` JSON format (version, sections, widgets)
   - Every widget type (`kpi`, `table`, `form`, `chart`, `text`) and its `config` shape
   - Output contract: respond with a JSON object containing `suggestedPageKey` (kebab-case), `suggestedName`, and `schema` (the `PageSchema` serialised as a string)

2. The API call targets `claude-opus-4-8` with `maxTokens=8000`. No `temperature`, `top_p`, or `top_k` are passed (all rejected by Opus 4.x with HTTP 400).

3. The response is parsed defensively: markdown code fences are stripped if present; `schema` may arrive as a JSON object or a quoted string — both forms are accepted and normalised.

4. The endpoint returns `GeneratedPageResponse(suggestedPageKey, suggestedName, schema)` without persisting the page — the user reviews the generated schema before saving.

**`AnthropicConfig`** registers an `AnthropicClient` Spring `@Bean` that reads the API key from the `ANTHROPIC_API_KEY` environment variable (overridable via `anthropic.api-key` Spring property), using `AnthropicOkHttpClient.fromEnv()` as the default.

### Frontend — `PageGenerator` component

A new `/generate` route in the portal renders `PageGenerator`:

1. User types a natural language description and clicks **Generate Page**.
2. The component calls `pageApi.generate(prompt)` and parses the `schema` string into a live `PageSchema`.
3. A full `PageRenderer` preview is rendered immediately — the user sees the actual page layout before saving.
4. The suggested `pageKey` and `name` are shown alongside the preview for review.
5. **Save Page** calls `pageApi.create(...)` to persist the page as a `DRAFT`, then navigates to `/pages/:pageKey`.

The generate call is separate from save so users can iterate on the prompt without creating duplicate database records.

## Consequences

**Positive**
- Non-technical operators can create functional dashboard pages from a description in seconds — no JSON authoring required
- The generated output is the same `PageSchema` that hand-authored pages use; `PageRenderer` requires no changes
- Anthropic Java SDK (`anthropic-java:2.34.0`) integrates cleanly as a Spring Bean; zero framework conflicts
- The `POST /generate` endpoint never persists data itself — generation is stateless and safe to retry
- Claude's instruction-following quality means the system prompt reliably constrains output to the required JSON shape; the parser handles edge cases (fenced code blocks, nested vs stringified schema) rather than failing hard

**Negative / trade-offs**
- **External API dependency**: page generation requires an outbound HTTPS call to `api.anthropic.com` and will fail if the network path is unavailable or the API key is not set. The endpoint should degrade gracefully (HTTP 503) rather than propagate the raw exception.
- **Latency**: a generation round-trip is typically 5–15 seconds. The UI shows a loading state; `POST /generate` is not suitable for synchronous workflows that require sub-second responses.
- **Cost**: each generation call consumes input tokens (system prompt ≈ 700 tokens + user prompt) and output tokens (schema ≈ 1,000–3,000 tokens). Rate limiting and tenant-level quotas should be considered before GA rollout.
- **Hallucinated API URLs**: Claude generates plausible-looking data-source URLs (e.g. `/api/v1/entities/order`) that may not exist. These are functional placeholders — widgets that cannot reach their data source will show empty/error states rather than crash.
- **No streaming**: the current implementation blocks until the full response arrives (`client.messages().create()`). For very long schemas, streaming (`createStreaming()`) would improve perceived responsiveness and should be considered in a follow-up.
