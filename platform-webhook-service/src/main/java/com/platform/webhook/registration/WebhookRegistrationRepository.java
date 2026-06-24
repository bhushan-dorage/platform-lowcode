package com.platform.webhook.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface WebhookRegistrationRepository extends JpaRepository<WebhookRegistration, String> {
    List<WebhookRegistration> findByTenantId(String tenantId);
    Optional<WebhookRegistration> findByIdAndTenantId(String id, String tenantId);

    @Query("SELECT w FROM WebhookRegistration w JOIN w.eventTypes et WHERE w.tenantId = :tenantId AND et = :eventType AND w.active = true")
    List<WebhookRegistration> findActiveByTenantIdAndEventType(String tenantId, String eventType);
}
