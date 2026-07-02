package com.platform.webhook.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookRegistrationService {

    private final WebhookRegistrationRepository repository;

    public List<WebhookRegistration> listForTenant(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public WebhookRegistration getForTenant(String id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + id));
    }

    @Transactional
    public WebhookRegistration create(WebhookRegistration reg) {
        reg.setActive(true);
        reg.setCreatedAt(Instant.now());
        return repository.save(reg);
    }

    @Transactional
    public WebhookRegistration update(String id, String tenantId, WebhookRegistration updated) {
        WebhookRegistration existing = getForTenant(id, tenantId);
        existing.setUrl(updated.getUrl());
        existing.setEventTypes(updated.getEventTypes());
        existing.setActive(updated.isActive());
        if (updated.getSecret() != null && !updated.getSecret().isBlank()) {
            existing.setSecret(updated.getSecret());
        }
        existing.setUpdatedAt(Instant.now());
        return repository.save(existing);
    }

    @Transactional
    public void delete(String id, String tenantId) {
        WebhookRegistration reg = getForTenant(id, tenantId);
        repository.delete(reg);
    }
}
