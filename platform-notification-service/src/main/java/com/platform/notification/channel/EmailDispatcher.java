package com.platform.notification.channel;

import com.platform.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDispatcher implements ChannelDispatcher {

    private final JavaMailSender mailSender;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DispatchResult dispatch(NotificationEvent event) {
        if (event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()) {
            return DispatchResult.failed(channel(), "No recipient email address");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getRecipientEmail());
            message.setSubject(event.getSubject() != null ? event.getSubject() : "(no subject)");
            message.setText(event.getBody() != null ? event.getBody() : "");
            mailSender.send(message);
            log.info("Email sent to {} for tenant {}", event.getRecipientEmail(), event.getTenantId());
            return DispatchResult.ok(channel());
        } catch (MailException e) {
            log.error("Failed to send email to {} for tenant {}: {}", event.getRecipientEmail(), event.getTenantId(), e.getMessage());
            return DispatchResult.failed(channel(), e.getMessage());
        }
    }
}
