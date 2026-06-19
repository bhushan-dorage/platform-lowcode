package com.platform.common.tenant;

/**
 * Subscription tier drives schema isolation strategy and feature gating.
 * ENTERPRISE/PROFESSIONAL get dedicated schemas; STARTER shares one.
 */
public enum TenantTier {
    ENTERPRISE,
    PROFESSIONAL,
    STARTER
}
