package com.platform.webhook.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.webhook.event.WebhookEvent;
import com.platform.webhook.registration.WebhookRegistration;
import com.platform.webhook.registration.WebhookRegistrationRepository;
import com.platform.webhook.signature.HmacSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDeliveryService {

    private static final int[] RETRY_DELAYS_SECONDS = {0, 60, 300, 900, 3600};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebhookRegistrationRepository registrationRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final HttpClient httpClient;
    private final DeliverySleeper sleeper;

    public void deliver(WebhookEvent event) {
        List<WebhookRegistration> registrations = registrationRepository
                .findActiveByTenantIdAndEventType(event.getTenantId(), event.getEventType());

        if (registrations.isEmpty()) {
            log.debug("No active webhooks for tenant {} event type {}", event.getTenantId(), event.getEventType());
            return;
        }

        String payload = serializePayload(event);
        for (WebhookRegistration registration : registrations) {
            deliverWithRetry(event, registration, payload);
        }
    }

    private void deliverWithRetry(WebhookEvent event, WebhookRegistration registration, String payload) {
        for (int attempt = 0; attempt < RETRY_DELAYS_SECONDS.length; attempt++) {
            if (attempt > 0) {
                int delaySeconds = RETRY_DELAYS_SECONDS[attempt];
                log.info("Retrying webhook delivery attempt {} for webhook {} after {}s",
                        attempt + 1, registration.getId(), delaySeconds);
                try {
                    sleeper.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            WebhookDeliveryLog log_entry = attemptDelivery(event, registration, payload, attempt + 1);
            if (log_entry.isSuccess()) {
                return;
            }
        }
        log.warn("All {} delivery attempts exhausted for webhook {} event {}",
                RETRY_DELAYS_SECONDS.length, registration.getId(), event.getEventId());
    }

    private WebhookDeliveryLog attemptDelivery(WebhookEvent event, WebhookRegistration registration,
                                                String payload, int attemptNumber) {
        WebhookDeliveryLog logEntry = new WebhookDeliveryLog();
        logEntry.setWebhookId(registration.getId());
        logEntry.setTenantId(event.getTenantId());
        logEntry.setEventId(event.getEventId());
        logEntry.setEventType(event.getEventType());
        logEntry.setUrl(registration.getUrl());
        logEntry.setAttemptNumber(attemptNumber);
        logEntry.setAttemptedAt(Instant.now());

        long startMs = System.currentTimeMillis();
        try {
            String signature = HmacSigner.sign(payload, registration.getSecret());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(registration.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-Platform-Signature", signature)
                    .header("X-Platform-Event-Id", event.getEventId())
                    .header("X-Platform-Event-Type", event.getEventType())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logEntry.setHttpStatusCode(response.statusCode());
            logEntry.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
            if (!logEntry.isSuccess()) {
                logEntry.setErrorMessage("HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(e.getMessage());
            log.error("Webhook delivery attempt {} failed for webhook {} event {}: {}",
                    attemptNumber, registration.getId(), event.getEventId(), e.getMessage());
        } finally {
            logEntry.setDurationMs(System.currentTimeMillis() - startMs);
            deliveryRepository.save(logEntry);
        }
        return logEntry;
    }

    private String serializePayload(WebhookEvent event) {
        try {
            return OBJECT_MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize webhook event", e);
        }
    }
}
