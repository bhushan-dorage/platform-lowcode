package com.platform.notification.preference;

import com.platform.notification.channel.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
    List<NotificationPreference> findByTenantIdAndUserId(String tenantId, String userId);
    Optional<NotificationPreference> findByTenantIdAndUserIdAndChannel(String tenantId, String userId, NotificationChannel channel);
}
