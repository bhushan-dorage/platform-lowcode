package com.platform.common.tenant;

/**
 * Resolves the subscription tier for a given tenant identifier.
 *
 * Implementations are expected to cache lookups; this interface is called on every
 * inbound request by {@link TenantResolutionFilter}.
 */
public interface TenantRegistry {

    /**
     * @param tenantId the raw tenant identifier from the X-Tenant-ID header
     * @return the resolved tier
     * @throws TenantNotFoundException if tenantId is unknown
     */
    TenantTier resolveTier(String tenantId);
}
