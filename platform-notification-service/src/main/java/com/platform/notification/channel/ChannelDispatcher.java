package com.platform.notification.channel;

import com.platform.notification.event.NotificationEvent;

public interface ChannelDispatcher {
    NotificationChannel channel();
    DispatchResult dispatch(NotificationEvent event);
}
