package com.platform.notification.inapp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, String> {
    List<InAppNotification> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, String userId);
    List<InAppNotification> findByTenantIdAndUserIdAndReadFalseOrderByCreatedAtDesc(String tenantId, String userId);
    long countByTenantIdAndUserIdAndReadFalse(String tenantId, String userId);
}
