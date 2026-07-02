package com.platform.webhook.registration;

import com.platform.webhook.delivery.WebhookDeliveryLog;
import com.platform.webhook.delivery.WebhookDeliveryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookRegistrationService registrationService;
    private final WebhookDeliveryRepository deliveryRepository;

    @GetMapping
    public List<WebhookRegistration> list(@AuthenticationPrincipal Jwt jwt) {
        return registrationService.listForTenant(jwt.getClaimAsString("tenant_id"));
    }

    @PostMapping
    public ResponseEntity<WebhookRegistration> create(
            @Valid @RequestBody WebhookRegistration reg,
            @AuthenticationPrincipal Jwt jwt) {
        reg.setTenantId(jwt.getClaimAsString("tenant_id"));
        reg.setCreatedBy(jwt.getSubject());
        WebhookRegistration created = registrationService.create(reg);
        return ResponseEntity.created(URI.create("/api/v1/webhooks/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    public WebhookRegistration get(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return registrationService.getForTenant(id, jwt.getClaimAsString("tenant_id"));
    }

    @PutMapping("/{id}")
    public WebhookRegistration update(
            @PathVariable String id,
            @RequestBody WebhookRegistration reg,
            @AuthenticationPrincipal Jwt jwt) {
        return registrationService.update(id, jwt.getClaimAsString("tenant_id"), reg);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        registrationService.delete(id, jwt.getClaimAsString("tenant_id"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    public List<WebhookDeliveryLog> getDeliveries(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        registrationService.getForTenant(id, jwt.getClaimAsString("tenant_id"));
        return deliveryRepository.findByWebhookIdOrderByAttemptedAtDesc(id);
    }
}
