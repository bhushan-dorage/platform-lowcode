package com.platform.workflow.deployment.messaging;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.DeploymentBuilder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes studio-backend's bundle-deploy request and turns the embedded BPMN resources into a
 * real Flowable deployment. Only BPMN artifact entries in the bundle are acted on — DMN/FORM/
 * DATA_MODEL/RULE_SET entries are out of scope for this consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BundleDeploymentConsumer {

    private final RepositoryService repositoryService;
    private final TenantAwareKafkaProducer kafkaProducer;
    private final TenantRegistry tenantRegistry;

    @KafkaListener(
            topicPattern = ".*\\.studio\\.deploy\\.events",
            groupId = "workflow-engine-bundle-deploy",
            containerFactory = "bundleDeployListenerContainerFactory"
    )
    public void consume(@Payload BundleDeployEvent event, Acknowledgment ack) {
        try {
            TenantContext.set(event.tenantId(), tenantRegistry.resolveTier(event.tenantId()));

            if (event.resources() == null || event.resources().isEmpty()) {
                log.info("Bundle {} has no BPMN resources to deploy — skipping", event.bundleId());
            } else {
                DeploymentBuilder builder = repositoryService.createDeployment()
                        .name("bundle-" + event.bundleId() + "-v" + event.version())
                        .tenantId(event.tenantId());
                for (BpmnResource resource : event.resources()) {
                    builder.addString(resource.name() + ".bpmn", resource.content());
                }
                builder.deploy();
                log.info("Deployed bundle bundleId={} version={} tenantId={} processCount={}",
                        event.bundleId(), event.version(), event.tenantId(), event.resources().size());
            }

            publishStatus(event, "DEPLOYED", null);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to deploy bundle bundleId={} tenantId={}", event.bundleId(), event.tenantId(), ex);
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
