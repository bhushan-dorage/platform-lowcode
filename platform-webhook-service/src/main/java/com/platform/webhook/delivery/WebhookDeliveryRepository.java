package com.platform.webhook.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDeliveryLog, String> {
    List<WebhookDeliveryLog> findByWebhookIdOrderByAttemptedAtDesc(String webhookId);
    List<WebhookDeliveryLog> findByTenantIdAndEventIdOrderByAttemptedAtDesc(String tenantId, String eventId);
    long countByWebhookIdAndSuccessTrue(String webhookId);
    long countByWebhookIdAndSuccessFalse(String webhookId);
}
