package com.platform.notification.channel;

import com.platform.notification.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationDispatcher {

    private final Map<NotificationChannel, ChannelDispatcher> dispatchers;

    public NotificationDispatcher(List<ChannelDispatcher> dispatchers) {
        this.dispatchers = dispatchers.stream()
                .collect(Collectors.toMap(ChannelDispatcher::channel, Function.identity()));
    }

    public List<DispatchResult> dispatch(NotificationEvent event) {
        List<String> channels = event.getChannels();
        if (channels == null || channels.isEmpty()) {
            log.warn("No channels specified for notification event {}", event.getEventId());
            return List.of();
        }
        return channels.stream()
                .map(ch -> {
                    try {
                        NotificationChannel channel = NotificationChannel.valueOf(ch);
                        ChannelDispatcher dispatcher = dispatchers.get(channel);
                        if (dispatcher == null) {
                            log.warn("No dispatcher found for channel {}", ch);
                            return DispatchResult.failed(channel, "No dispatcher found");
                        }
                        return dispatcher.dispatch(event);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown channel: {}", ch);
                        return DispatchResult.failed(NotificationChannel.EMAIL, "Unknown channel: " + ch);
                    }
                })
                .collect(Collectors.toList());
    }
}
