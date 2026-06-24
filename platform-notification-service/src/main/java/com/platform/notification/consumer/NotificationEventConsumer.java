package com.platform.notification.consumer;

import com.platform.notification.channel.DispatchResult;
import com.platform.notification.channel.NotificationDispatcher;
import com.platform.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = "platform.notification.events", groupId = "notification-service")
    public void consume(NotificationEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("Received null or malformed notification event, skipping");
            return;
        }
        log.info("Processing notification event {} for tenant {}", event.getEventId(), event.getTenantId());
        try {
            List<DispatchResult> results = dispatcher.dispatch(event);
            long failures = results.stream().filter(r -> !r.success()).count();
            if (failures > 0) {
                log.warn("Notification event {} had {} dispatch failure(s) out of {}",
                        event.getEventId(), failures, results.size());
            } else {
                log.info("Notification event {} dispatched successfully across {} channel(s)",
                        event.getEventId(), results.size());
            }
        } catch (Exception e) {
            log.error("Unhandled error processing notification event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }
}
