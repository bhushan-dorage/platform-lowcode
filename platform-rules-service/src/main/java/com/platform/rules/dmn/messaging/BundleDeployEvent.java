package com.platform.rules.dmn.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Consumer-side view of the event studio-backend's BundleService.deployBundle() publishes on
 * "{tenantId}.studio.deploy.events". Deliberately defined locally rather than shared with
 * platform-studio-backend — the two services only agree on the JSON wire shape, not a Java
 * class. The producer's payload also carries a "bpmnResources" field (consumed by
 * platform-workflow-engine) that this record deliberately omits; ignoreUnknown guards against
 * relying on that being silently dropped by default Jackson config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BundleDeployEvent(
        String eventType,
        String bundleId,
        String tenantId,
        String version,
        Map<String, String> artifactVersions,
        List<DmnResource> dmnResources,
        String requestedBy,
        Instant requestedAt
) {}
