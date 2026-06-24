package com.platform.notification.controller;

import com.platform.notification.channel.NotificationChannel;
import com.platform.notification.inapp.InAppNotification;
import com.platform.notification.inapp.InAppNotificationRepository;
import com.platform.notification.preference.NotificationPreference;
import com.platform.notification.preference.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationRepository inAppRepository;
    private final NotificationPreferenceService preferenceService;

    @GetMapping("/in-app")
    public List<InAppNotification> getInAppNotifications(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        String userId = jwt.getSubject();
        return inAppRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
    }

    @GetMapping("/in-app/unread")
    public List<InAppNotification> getUnreadNotifications(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        String userId = jwt.getSubject();
        return inAppRepository.findByTenantIdAndUserIdAndReadFalseOrderByCreatedAtDesc(tenantId, userId);
    }

    @GetMapping("/in-app/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        String userId = jwt.getSubject();
        long count = inAppRepository.countByTenantIdAndUserIdAndReadFalse(tenantId, userId);
        return Map.of("count", count);
    }

    @PostMapping("/in-app/{id}/read")
    public ResponseEntity<InAppNotification> markRead(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return inAppRepository.findById(id)
                .filter(n -> n.getTenantId().equals(jwt.getClaimAsString("tenant_id"))
                        && n.getUserId().equals(jwt.getSubject()))
                .map(n -> {
                    n.setRead(true);
                    n.setReadAt(Instant.now());
                    return ResponseEntity.ok(inAppRepository.save(n));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/preferences")
    public List<NotificationPreference> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        return preferenceService.getPreferences(
                jwt.getClaimAsString("tenant_id"), jwt.getSubject());
    }

    @PutMapping("/preferences/{channel}")
    public NotificationPreference setPreference(
            @PathVariable String channel,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal Jwt jwt) {
        return preferenceService.setPreference(
                jwt.getClaimAsString("tenant_id"),
                jwt.getSubject(),
                NotificationChannel.valueOf(channel.toUpperCase()),
                enabled);
    }
}
