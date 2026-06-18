package com.platform.common.tenant;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }
}
