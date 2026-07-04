package com.platform.workflow.deployment.messaging;

import java.time.Instant;

/** Published back to "{tenantId}.studio.deploy.status.events" so studio-backend can update DeploymentBundle.status. */
public record DeploymentStatusEvent(
        String bundleId,
        String tenantId,
        String status,
        String errorMessage,
        Instant completedAt
) {}
