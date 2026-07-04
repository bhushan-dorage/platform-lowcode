package com.platform.studio.artifact.messaging;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantRegistry;
import com.platform.studio.artifact.domain.BundleStatus;
import com.platform.studio.artifact.domain.DeploymentBundle;
import com.platform.studio.artifact.repository.DeploymentBundleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Applies platform-workflow-engine's bundle-deploy outcome back onto the DeploymentBundle it tracks. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeploymentStatusConsumer {

    private final DeploymentBundleRepository bundleRepo;
    private final TenantRegistry tenantRegistry;

    @KafkaListener(
            topicPattern = ".*\\.studio\\.deploy\\.status\\.events",
            groupId = "studio-backend-deploy-status",
            containerFactory = "deploymentStatusListenerContainerFactory"
    )
    @Transactional
    public void consume(@Payload DeploymentStatusEvent event, Acknowledgment ack) {
        try {
            TenantContext.set(event.tenantId(), tenantRegistry.resolveTier(event.tenantId()));

            DeploymentBundle bundle = bundleRepo
                    .findByIdAndTenantId(UUID.fromString(event.bundleId()), event.tenantId())
                    .orElse(null);
            if (bundle == null) {
                log.warn("Received deploy status for unknown bundleId={} tenantId={}",
                        event.bundleId(), event.tenantId());
                ack.acknowledge();
                return;
            }

            if ("DEPLOYED".equals(event.status())) {
                bundle.setStatus(BundleStatus.DEPLOYED);
                bundle.setDeployedAt(event.completedAt());
                bundle.setDeployError(null);
            } else {
                bundle.setStatus(BundleStatus.FAILED);
                bundle.setDeployError(event.errorMessage());
            }
            bundleRepo.save(bundle);
            log.info("Applied deploy status bundleId={} status={}", event.bundleId(), event.status());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to apply deploy status for bundleId={}", event.bundleId(), ex);
            ack.acknowledge(); // ack to avoid reprocessing a bad message indefinitely
        } finally {
            TenantContext.clear();
        }
    }
}
