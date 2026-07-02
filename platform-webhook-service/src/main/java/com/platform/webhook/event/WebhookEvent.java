package com.platform.webhook.event;

import lombok.Data;
import java.util.Map;

@Data
public class WebhookEvent {
    private String eventId;
    private String tenantId;
    private String eventType;
    private Map<String, Object> payload;
}
