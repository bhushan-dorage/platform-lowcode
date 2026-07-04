package com.platform.studio.artifact.messaging;

import java.time.Instant;

/** Consumer-side view of the status platform-workflow-engine publishes after a bundle deploy attempt. */
public record DeploymentStatusEvent(
        String bundleId,
        String tenantId,
        String status,
        String errorMessage,
        Instant completedAt
) {}
