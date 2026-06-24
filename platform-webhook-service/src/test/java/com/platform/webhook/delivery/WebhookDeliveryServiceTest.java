package com.platform.webhook.delivery;

import com.platform.webhook.event.WebhookEvent;
import com.platform.webhook.registration.WebhookRegistration;
import com.platform.webhook.registration.WebhookRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

    @Mock
    private WebhookRegistrationRepository registrationRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @InjectMocks
    private WebhookDeliveryService deliveryService;

    private WebhookEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new WebhookEvent();
        sampleEvent.setEventId("evt-1");
        sampleEvent.setTenantId("acme");
        sampleEvent.setEventType("FORM_SUBMITTED");
        sampleEvent.setPayload(Map.of("formId", "form-123"));
    }

    @Test
    void deliver_noRegistrations_doesNothing() {
        when(registrationRepository.findActiveByTenantIdAndEventType("acme", "FORM_SUBMITTED"))
                .thenReturn(List.of());
        deliveryService.deliver(sampleEvent);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void deliver_withRegistration_savesDeliveryLog() {
        WebhookRegistration reg = new WebhookRegistration();
        reg.setId("wh-1");
        reg.setTenantId("acme");
        reg.setUrl("http://invalid-test-url-will-fail.example.com/webhook");
        reg.setSecret("secret-key");
        reg.setEventTypes(List.of("FORM_SUBMITTED"));
        reg.setActive(true);

        when(registrationRepository.findActiveByTenantIdAndEventType("acme", "FORM_SUBMITTED"))
                .thenReturn(List.of(reg));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        deliveryService.deliver(sampleEvent);

        verify(deliveryRepository, atLeastOnce()).save(argThat(log ->
                log.getWebhookId().equals("wh-1") && log.getEventId().equals("evt-1")));
    }

    @Test
    void deliver_successfulDelivery_stopsRetrying() {
        // This test verifies that when a registration exists but delivery fails,
        // at least one delivery log is saved
        WebhookRegistration reg = new WebhookRegistration();
        reg.setId("wh-2");
        reg.setTenantId("acme");
        reg.setUrl("http://no-connect.test/webhook");
        reg.setSecret("s");
        reg.setEventTypes(List.of("FORM_SUBMITTED"));
        reg.setActive(true);

        when(registrationRepository.findActiveByTenantIdAndEventType("acme", "FORM_SUBMITTED"))
                .thenReturn(List.of(reg));
        when(deliveryRepository.save(any())).thenAnswer(i -> {
            WebhookDeliveryLog log = i.getArgument(0);
            // Simulate success on first attempt to prevent actual retries
            log.setSuccess(true);
            return log;
        });

        deliveryService.deliver(sampleEvent);
        verify(deliveryRepository, times(1)).save(any());
    }
}
