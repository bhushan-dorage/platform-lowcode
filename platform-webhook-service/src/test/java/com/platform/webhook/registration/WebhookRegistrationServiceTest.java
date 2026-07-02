package com.platform.webhook.registration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookRegistrationServiceTest {

    @Mock
    private WebhookRegistrationRepository repository;

    @InjectMocks
    private WebhookRegistrationService service;

    private WebhookRegistration sample;

    @BeforeEach
    void setUp() {
        sample = new WebhookRegistration();
        sample.setId("wh-1");
        sample.setTenantId("acme");
        sample.setUrl("https://example.com/hook");
        sample.setSecret("s3cr3t");
        sample.setEventTypes(List.of("FORM_SUBMITTED"));
        sample.setCreatedBy("user-1");
    }

    @Test
    void create_setsActiveTrue() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        WebhookRegistration result = service.create(sample);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void listForTenant_delegatesToRepository() {
        when(repository.findByTenantId("acme")).thenReturn(List.of(sample));
        assertThat(service.listForTenant("acme")).containsExactly(sample);
    }

    @Test
    void getForTenant_throwsWhenNotFound() {
        when(repository.findByIdAndTenantId("missing", "acme")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getForTenant("missing", "acme"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Webhook not found");
    }

    @Test
    void delete_callsRepositoryDelete() {
        when(repository.findByIdAndTenantId("wh-1", "acme")).thenReturn(Optional.of(sample));
        service.delete("wh-1", "acme");
        verify(repository).delete(sample);
    }

    @Test
    void update_changesUrlAndEventTypes() {
        when(repository.findByIdAndTenantId("wh-1", "acme")).thenReturn(Optional.of(sample));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        WebhookRegistration update = new WebhookRegistration();
        update.setUrl("https://new-url.com/hook");
        update.setEventTypes(List.of("ENTITY_CREATED", "ENTITY_UPDATED"));
        update.setActive(true);

        WebhookRegistration result = service.update("wh-1", "acme", update);
        assertThat(result.getUrl()).isEqualTo("https://new-url.com/hook");
        assertThat(result.getEventTypes()).containsExactly("ENTITY_CREATED", "ENTITY_UPDATED");
    }
}
