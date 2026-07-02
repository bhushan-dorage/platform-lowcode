package com.platform.notification.event;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class NotificationEvent {
    private String eventId;
    private String tenantId;
    private String notificationType;
    private List<String> channels;
    private List<String> recipientUserIds;
    private String recipientEmail;
    private String subject;
    private String body;
    private Map<String, String> templateVariables;
    private String templateId;
}
