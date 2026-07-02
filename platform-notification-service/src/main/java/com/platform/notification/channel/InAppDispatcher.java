package com.platform.notification.channel;

import com.platform.notification.event.NotificationEvent;
import com.platform.notification.inapp.InAppNotification;
import com.platform.notification.inapp.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InAppDispatcher implements ChannelDispatcher {

    private final InAppNotificationRepository repository;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public DispatchResult dispatch(NotificationEvent event) {
        if (event.getRecipientUserIds() == null || event.getRecipientUserIds().isEmpty()) {
            return DispatchResult.failed(channel(), "No recipient user IDs for in-app notification");
        }
        try {
            for (String userId : event.getRecipientUserIds()) {
                InAppNotification notification = new InAppNotification();
                notification.setTenantId(event.getTenantId());
                notification.setUserId(userId);
                notification.setSubject(event.getSubject() != null ? event.getSubject() : "Notification");
                notification.setBody(event.getBody() != null ? event.getBody() : "");
                repository.save(notification);
            }
            log.info("In-app notifications stored for {} recipients in tenant {}",
                    event.getRecipientUserIds().size(), event.getTenantId());
            return DispatchResult.ok(channel());
        } catch (Exception e) {
            log.error("Failed to store in-app notification for tenant {}: {}", event.getTenantId(), e.getMessage());
            return DispatchResult.failed(channel(), e.getMessage());
        }
    }
}
