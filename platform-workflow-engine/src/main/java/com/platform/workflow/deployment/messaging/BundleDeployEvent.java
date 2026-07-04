package com.platform.workflow.deployment.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Consumer-side view of the event studio-backend's BundleService.deployBundle() publishes on
 * "{tenantId}.studio.deploy.events". Deliberately defined locally rather than shared with
 * platform-studio-backend — the two services only agree on the JSON wire shape, not a Java class.
 */
public record BundleDeployEvent(
        String eventType,
        String bundleId,
        String tenantId,
        String version,
        Map<String, String> artifactVersions,
        List<BpmnResource> resources,
        String requestedBy,
        Instant requestedAt
) {}
