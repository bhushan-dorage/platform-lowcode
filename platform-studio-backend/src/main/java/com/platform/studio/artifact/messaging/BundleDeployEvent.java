package com.platform.studio.artifact.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Published on "{tenantId}.studio.deploy.events". BPMN content is embedded directly (rather than
 * a reference workflow-engine would have to fetch back over REST) so a deploy doesn't depend on
 * studio-backend being reachable at the moment workflow-engine processes the event.
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
