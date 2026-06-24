package com.platform.notification.channel;

import com.platform.notification.event.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailDispatcherTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailDispatcher dispatcher;

    @Test
    void dispatch_sendsEmailSuccessfully() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setTenantId("acme");
        event.setRecipientEmail("user@example.com");
        event.setSubject("Test Subject");
        event.setBody("Test body");

        DispatchResult result = dispatcher.dispatch(event);
        assertThat(result.success()).isTrue();
        assertThat(result.channel()).isEqualTo(NotificationChannel.EMAIL);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void dispatch_missingEmail_returnsFailure() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setTenantId("acme");
        event.setRecipientEmail(null);

        DispatchResult result = dispatcher.dispatch(event);
        assertThat(result.success()).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void dispatch_mailException_returnsFailure() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("evt-1");
        event.setTenantId("acme");
        event.setRecipientEmail("user@example.com");
        event.setSubject("Test");
        event.setBody("Body");
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        DispatchResult result = dispatcher.dispatch(event);
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("SMTP");
    }

    @Test
    void channel_returnsEmail() {
        assertThat(dispatcher.channel()).isEqualTo(NotificationChannel.EMAIL);
    }
}
