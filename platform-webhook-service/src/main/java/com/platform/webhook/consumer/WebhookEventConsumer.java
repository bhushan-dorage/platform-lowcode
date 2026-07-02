package com.platform.webhook.consumer;

import com.platform.webhook.delivery.WebhookDeliveryService;
import com.platform.webhook.event.WebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookEventConsumer {

    private final WebhookDeliveryService deliveryService;

    @KafkaListener(topics = "platform.webhook.events", groupId = "webhook-service")
    public void consume(WebhookEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("Received null or malformed webhook event, skipping");
            return;
        }
        log.info("Processing webhook event {} type {} for tenant {}",
                event.getEventId(), event.getEventType(), event.getTenantId());
        try {
            deliveryService.deliver(event);
        } catch (Exception e) {
            log.error("Unhandled error processing webhook event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }
}
