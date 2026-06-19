package com.platform.entitlements.enforcement;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.entitlements.cache.EntitlementsCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final EntitlementsCacheService cacheService;
    private final TenantAwareKafkaProducer kafkaProducer;

    public boolean hasPermission(String userId, String permission) {
        Set<String> effective = cacheService.getEffectivePermissions(TenantContext.getTenantId(), userId);
        return effective.contains(permission);
    }

    /**
     * Asserts permission; publishes ACCESS_DENIED audit event on denial.
     */
    public void assertPermission(String userId, String permission, String resourceType, String resourceId) {
        if (!hasPermission(userId, permission)) {
            publishAccessDenied(userId, permission, resourceType, resourceId, "PERMISSION_NOT_GRANTED");
            throw new AccessDeniedException("User " + userId + " lacks permission: " + permission);
        }
    }

    private void publishAccessDenied(String userId, String permission, String resourceType,
                                      String resourceId, String reason) {
        kafkaProducer.send("audit.events", userId, Map.of(
                "eventType", "ACCESS_DENIED",
                "domain", "ACCESS",
                "tenantId", TenantContext.getTenantId(),
                "timestamp", Instant.now().toString(),
                "actor", Map.of("userId", userId),
                "resource", Map.of("type", resourceType, "id", resourceId != null ? resourceId : ""),
                "action", Map.of("operation", permission, "denialReason", reason)
        ));
    }
}
