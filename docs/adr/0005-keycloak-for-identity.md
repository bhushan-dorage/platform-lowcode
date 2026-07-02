# ADR-0005: Keycloak as the Identity and Access Management Provider

**Status:** Accepted  
**Date:** 2026-01-15

## Context

The platform needs an identity provider (IdP) that supports:

- OIDC / OAuth 2.0 with PKCE for single-page applications
- Client credentials grant for service-to-service calls
- Multi-tenant realm isolation — each tenant gets its own Keycloak realm
- RBAC at the IdP level (realm roles, client roles) feeding into the platform's own entitlements engine
- SAML 2.0 federation for enterprise customers with existing corporate IdPs (AD FS, Okta, Azure AD)
- Self-hosted option for data-residency and compliance requirements

Candidates evaluated:

| Provider | Notes |
|---|---|
| **Keycloak** | Open-source, self-hosted, full OIDC/SAML, multi-realm, strong Spring Security integration |
| Auth0 | Excellent DX, managed SaaS, but per-MAU pricing becomes expensive at enterprise scale |
| Okta | Enterprise-grade, but SaaS-only, high cost, and vendor lock-in |
| Azure AD B2C | Good for Microsoft-ecosystem tenants; complex pricing and limited customisation |
| AWS Cognito | Managed, low cost, but limited SAML federation and poor customisation for complex flows |
| custom JWT | Maximum control, but reinventing a security-critical wheel — not viable |

## Decision

Use **Keycloak 23.x** as the platform's identity provider.

- Each tenant gets a dedicated Keycloak realm (`{tenantId}`) seeded at onboarding time
- SPAs (Portal, Studio) use **PKCE** (`response_type=code` + `code_challenge_method=S256`) — no client secret in the browser
- Backend services use **client credentials** grant with a per-service Keycloak client
- Spring Security OAuth2 resource server validates JWT signatures against Keycloak's JWKS endpoint (`{issuer}/.well-known/openid-configuration`)
- Keycloak groups and realm roles are included in the JWT and consumed by `platform-entitlements-service` as the bootstrap identity for RBAC evaluation
- Realm JSON (`infra/keycloak/realm.json`) is imported at startup for reproducible local dev

## Consequences

**Positive:**
- Full OIDC compliance — any OIDC-compliant client library works with no platform-specific code
- PKCE eliminates implicit flow vulnerabilities in SPAs
- Keycloak's identity brokering allows enterprise tenants to federate their existing corporate IdP (AD FS, Okta, Azure AD) without platform code changes
- Realm-per-tenant provides hard isolation: a misconfiguration in one tenant's realm cannot affect another
- Self-hosted satisfies data-residency requirements (tenant user data stays on-premises if needed)
- Keycloak Operator available for Kubernetes deployment

**Negative:**
- Keycloak is a large operational dependency (~512 MB heap in production) — adds to infrastructure cost
- Realm-per-tenant creates an admin overhead: realm provisioning, client configuration, and role seeding must be automated (and is, via `tenant-onboarding.md` playbook)
- Keycloak upgrade path can be complex — realm export/import required between major versions
- Startup time in dev (~60s for Keycloak to be ready) slows the local docker-compose workflow
- JWKS key rotation requires all services to refresh their cached public keys — handled automatically by Spring Security's `NimbusJwtDecoder` with a 5-minute cache TTL
