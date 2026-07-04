package com.platform.studio.artifact.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Published on "{tenantId}.studio.deploy.events". BPMN/DMN content is embedded directly (rather
 * than a reference the consuming engine would have to fetch back over REST) so a deploy doesn't
 * depend on studio-backend being reachable at the moment workflow-engine/rules-service process
 * the event. platform-workflow-engine only reads "resources" (BPMN); platform-rules-service only
 * reads "dmnResources" — each service's local copy of this record omits the field it doesn't need.
 */
public record BundleDeployEvent(
        String eventType,
        String bundleId,
        String tenantId,
        String version,
        Map<String, String> artifactVersions,
        List<BpmnResource> resources,
        List<DmnResource> dmnResources,
        String requestedBy,
        Instant requestedAt
) {}
