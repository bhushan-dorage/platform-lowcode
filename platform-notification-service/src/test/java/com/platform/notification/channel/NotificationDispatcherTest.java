package com.platform.notification.channel;

import com.platform.notification.event.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private EmailDispatcher emailDispatcher;

    @Mock
    private SmsDispatcher smsDispatcher;

    @Mock
    private PushDispatcher pushDispatcher;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(emailDispatcher.channel()).thenReturn(NotificationChannel.EMAIL);
        when(smsDispatcher.channel()).thenReturn(NotificationChannel.SMS);
        when(pushDispatcher.channel()).thenReturn(NotificationChannel.PUSH);
        dispatcher = new NotificationDispatcher(List.of(emailDispatcher, smsDispatcher, pushDispatcher));
    }

    @Test
    void dispatch_noChannels_returnsEmpty() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setChannels(List.of());
        assertThat(dispatcher.dispatch(event)).isEmpty();
    }

    @Test
    void dispatch_emailChannel_delegatesToEmailDispatcher() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setTenantId("acme");
        event.setChannels(List.of("EMAIL"));
        when(emailDispatcher.dispatch(event)).thenReturn(DispatchResult.ok(NotificationChannel.EMAIL));
        List<DispatchResult> results = dispatcher.dispatch(event);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isTrue();
        verify(emailDispatcher).dispatch(event);
    }

    @Test
    void dispatch_multipleChannels_dispatchesAll() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setTenantId("acme");
        event.setChannels(List.of("EMAIL", "SMS"));
        when(emailDispatcher.dispatch(event)).thenReturn(DispatchResult.ok(NotificationChannel.EMAIL));
        when(smsDispatcher.dispatch(event)).thenReturn(DispatchResult.ok(NotificationChannel.SMS));
        List<DispatchResult> results = dispatcher.dispatch(event);
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(DispatchResult::success);
    }

    @Test
    void dispatch_unknownChannel_returnsFailure() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setTenantId("acme");
        event.setChannels(List.of("UNKNOWN_CHANNEL"));
        List<DispatchResult> results = dispatcher.dispatch(event);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isFalse();
    }

    @Test
    void dispatch_nullChannels_returnsEmpty() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setChannels(null);
        assertThat(dispatcher.dispatch(event)).isEmpty();
    }
}
