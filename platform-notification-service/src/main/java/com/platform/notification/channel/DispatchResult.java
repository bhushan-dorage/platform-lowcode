package com.platform.notification.channel;

public record DispatchResult(NotificationChannel channel, boolean success, String errorMessage) {
    public static DispatchResult ok(NotificationChannel channel) {
        return new DispatchResult(channel, true, null);
    }
    public static DispatchResult failed(NotificationChannel channel, String error) {
        return new DispatchResult(channel, false, error);
    }
}
