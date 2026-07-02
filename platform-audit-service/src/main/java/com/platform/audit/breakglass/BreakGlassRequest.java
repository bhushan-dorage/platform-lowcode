package com.platform.audit.breakglass;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the break-glass emergency access endpoint.
 *
 * Every break-glass access is immediately recorded as an immutable audit event so
 * that the action is traceable regardless of what the caller does with the access.
 */
public record BreakGlassRequest(
        @NotBlank String targetTenantId,
        @NotBlank String reason,
        String targetResourceType,
        String targetResourceId
) {}
