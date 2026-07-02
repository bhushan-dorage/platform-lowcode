package com.platform.notification.channel;

import com.platform.notification.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsDispatcher implements ChannelDispatcher {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public DispatchResult dispatch(NotificationEvent event) {
        log.info("[STUB] SMS dispatch for tenant {} event {}", event.getTenantId(), event.getEventId());
        return DispatchResult.ok(channel());
    }
}
