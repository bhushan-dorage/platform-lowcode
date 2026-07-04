package com.platform.rules.dmn.messaging;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.dmn.api.DmnRepositoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes studio-backend's bundle-deploy request and turns the embedded DMN resources into a
 * real Flowable DMN deployment. Only DMN artifact entries in the bundle are acted on — BPMN is
 * handled independently by platform-workflow-engine's own consumer of the same topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DmnDeploymentConsumer {

    private final DmnRepositoryService dmnRepositoryService;
    private final TenantAwareKafkaProducer kafkaProducer;
    private final TenantRegistry tenantRegistry;

    @KafkaListener(
            topicPattern = ".*\\.studio\\.deploy\\.events",
            groupId = "rules-service-bundle-deploy",
            containerFactory = "dmnBundleDeployListenerContainerFactory"
    )
    public void consume(@Payload BundleDeployEvent event, Acknowledgment ack) {
        try {
            TenantContext.set(event.tenantId(), tenantRegistry.resolveTier(event.tenantId()));

            if (event.dmnResources() == null || event.dmnResources().isEmpty()) {
                log.info("Bundle {} has no DMN resources to deploy — skipping", event.bundleId());
            } else {
                var builder = dmnRepositoryService.createDeployment()
                        .name("bundle-" + event.bundleId() + "-v" + event.version())
                        .tenantId(event.tenantId());
                for (DmnResource resource : event.dmnResources()) {
                    builder.addString(resource.name() + ".dmn", resource.content());
                }
                builder.deploy();
                log.info("Deployed DMN bundleId={} version={} tenantId={} decisionCount={}",
                        event.bundleId(), event.version(), event.tenantId(), event.dmnResources().size());
            }

            publishStatus(event, "DEPLOYED", null);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to deploy DMN bundleId={} tenantId={}", event.bundleId(), event.tenantId(), ex);
            publishStatus(event, "FAILED", ex.getMessage());
            ack.acknowledge(); // ack to avoid reprocessing a bad message indefinitely
        } finally {
            TenantContext.clear();
        }
    }

    private void publishStatus(BundleDeployEvent event, String status, String errorMessage) {
        try {
            TenantContext.set(event.tenantId(), tenantRegistry.resolveTier(event.tenantId()));
            kafkaProducer.send("studio.deploy.status.events", event.bundleId(),
                    new DeploymentStatusEvent(event.bundleId(), event.tenantId(), status, errorMessage, Instant.now()));
        } catch (Exception e) {
            log.error("Failed to publish deploy status for bundleId={}", event.bundleId(), e);
        }
    }
}
