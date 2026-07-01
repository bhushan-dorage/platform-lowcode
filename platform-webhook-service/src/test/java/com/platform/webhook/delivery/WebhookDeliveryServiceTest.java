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

import com.platform.webhook.delivery.DeliverySleeper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    @Mock
    private HttpClient httpClient;

    @Mock
    private DeliverySleeper sleeper;

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

    @SuppressWarnings("unchecked")
    @Test
    void deliver_withRegistration_successOnFirstAttempt() throws Exception {
        WebhookRegistration reg = buildRegistration("wh-1");

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        when(registrationRepository.findActiveByTenantIdAndEventType("acme", "FORM_SUBMITTED"))
                .thenReturn(List.of(reg));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        deliveryService.deliver(sampleEvent);

        verify(deliveryRepository, times(1)).save(argThat(log ->
                log.getWebhookId().equals("wh-1") && log.getEventId().equals("evt-1") && log.isSuccess()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void deliver_retriesOn5xx_stopsWhenSuccessful() throws Exception {
        WebhookRegistration reg = buildRegistration("wh-2");

        HttpResponse<String> failResponse = mock(HttpResponse.class);
        when(failResponse.statusCode()).thenReturn(500);
        HttpResponse<String> okResponse = mock(HttpResponse.class);
        when(okResponse.statusCode()).thenReturn(200);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(failResponse)
                .thenReturn(okResponse);

        when(registrationRepository.findActiveByTenantIdAndEventType("acme", "FORM_SUBMITTED"))
                .thenReturn(List.of(reg));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        deliveryService.deliver(sampleEvent);

        verify(deliveryRepository, times(2)).save(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void deliver_allAttemptsExhausted_savesAllLogs() throws Exception {
        WebhookRegistration reg = buildRegistration("wh-3");

        HttpResponse<String> failResponse = mock(HttpResponse.class);
        when(failResponse.statusCode()).thenReturn(503);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(failResponse);

        when(registrationRepository.findActiveByTenantIdAndEventType("acme", "FORM_SUBMITTED"))
                .thenReturn(List.of(reg));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        deliveryService.deliver(sampleEvent);

        verify(deliveryRepository, times(5)).save(any());
    }

    private WebhookRegistration buildRegistration(String id) {
        WebhookRegistration reg = new WebhookRegistration();
        reg.setId(id);
        reg.setTenantId("acme");
        reg.setUrl("http://test-endpoint.example.com/webhook");
        reg.setSecret("secret-key");
        reg.setEventTypes(List.of("FORM_SUBMITTED"));
        reg.setActive(true);
        return reg;
    }
}
