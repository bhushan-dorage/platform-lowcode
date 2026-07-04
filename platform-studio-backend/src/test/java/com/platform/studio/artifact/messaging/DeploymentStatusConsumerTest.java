package com.platform.studio.artifact.messaging;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantRegistry;
import com.platform.common.tenant.TenantTier;
import com.platform.studio.artifact.domain.BundleStatus;
import com.platform.studio.artifact.domain.DeploymentBundle;
import com.platform.studio.artifact.repository.DeploymentBundleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentStatusConsumerTest {

    @Mock DeploymentBundleRepository bundleRepo;
    @Mock TenantRegistry tenantRegistry;
    @Mock Acknowledgment ack;
    @InjectMocks DeploymentStatusConsumer consumer;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void deployedStatus_marksBundleDeployed() {
        UUID bundleId = UUID.randomUUID();
        DeploymentBundle bundle = new DeploymentBundle();
        bundle.setId(bundleId);
        bundle.setTenantId("acme");
        bundle.setStatus(BundleStatus.DEPLOYING);

        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.PROFESSIONAL);
        when(bundleRepo.findByIdAndTenantId(bundleId, "acme")).thenReturn(Optional.of(bundle));

        Instant completedAt = Instant.now();
        consumer.consume(new DeploymentStatusEvent(bundleId.toString(), "acme", "DEPLOYED", null, completedAt), ack);

        assertThat(bundle.getStatus()).isEqualTo(BundleStatus.DEPLOYED);
        assertThat(bundle.getDeployedAt()).isEqualTo(completedAt);
        assertThat(bundle.getDeployError()).isNull();
        verify(bundleRepo).save(bundle);
        verify(ack).acknowledge();
    }

    @Test
    void failedStatus_marksBundleFailedWithError() {
        UUID bundleId = UUID.randomUUID();
        DeploymentBundle bundle = new DeploymentBundle();
        bundle.setId(bundleId);
        bundle.setTenantId("acme");
        bundle.setStatus(BundleStatus.DEPLOYING);

        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.PROFESSIONAL);
        when(bundleRepo.findByIdAndTenantId(bundleId, "acme")).thenReturn(Optional.of(bundle));

        consumer.consume(new DeploymentStatusEvent(bundleId.toString(), "acme", "FAILED", "boom", Instant.now()), ack);

        assertThat(bundle.getStatus()).isEqualTo(BundleStatus.FAILED);
        assertThat(bundle.getDeployError()).isEqualTo("boom");
        verify(ack).acknowledge();
    }

    @Test
    void unknownBundle_acksWithoutThrowing() {
        UUID bundleId = UUID.randomUUID();
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.PROFESSIONAL);
        when(bundleRepo.findByIdAndTenantId(bundleId, "acme")).thenReturn(Optional.empty());

        consumer.consume(new DeploymentStatusEvent(bundleId.toString(), "acme", "DEPLOYED", null, Instant.now()), ack);

        verify(ack).acknowledge();
    }
}
