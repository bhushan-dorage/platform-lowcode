package com.platform.notification.preference;

import com.platform.notification.channel.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public List<NotificationPreference> getPreferences(String tenantId, String userId) {
        return repository.findByTenantIdAndUserId(tenantId, userId);
    }

    public NotificationPreference setPreference(String tenantId, String userId, NotificationChannel channel, boolean enabled) {
        NotificationPreference pref = repository
                .findByTenantIdAndUserIdAndChannel(tenantId, userId, channel)
                .orElseGet(() -> {
                    NotificationPreference p = new NotificationPreference();
                    p.setTenantId(tenantId);
                    p.setUserId(userId);
                    p.setChannel(channel);
                    return p;
                });
        pref.setEnabled(enabled);
        return repository.save(pref);
    }
}
