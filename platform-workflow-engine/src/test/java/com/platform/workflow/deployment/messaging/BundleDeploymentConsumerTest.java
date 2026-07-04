package com.platform.workflow.deployment.messaging;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantRegistry;
import com.platform.common.tenant.TenantTier;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.DeploymentBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BundleDeploymentConsumerTest {

    @Mock private RepositoryService repositoryService;
    @Mock private TenantAwareKafkaProducer kafkaProducer;
    @Mock private TenantRegistry tenantRegistry;
    @Mock private Acknowledgment ack;

    @InjectMocks private BundleDeploymentConsumer consumer;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void deploysBpmnResourcesAndPublishesDeployedStatus() {
        DeploymentBuilder builder = mock(DeploymentBuilder.class, RETURNS_SELF);
        when(repositoryService.createDeployment()).thenReturn(builder);
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);

        BundleDeployEvent event = new BundleDeployEvent(
                "BUNDLE_DEPLOY_REQUESTED", "bundle-1", "acme", "1.0.0",
                Map.of("artifact-1", "1.0.0"),
                List.of(new BpmnResource("loan-approval", "<xml/>")),
                "alice", Instant.now());

        consumer.consume(event, ack);

        verify(builder).tenantId("acme");
        verify(builder).addString("loan-approval.bpmn", "<xml/>");
        verify(builder).deploy();
        verify(ack).acknowledge();

        ArgumentCaptor<DeploymentStatusEvent> captor = ArgumentCaptor.forClass(DeploymentStatusEvent.class);
        verify(kafkaProducer).send(eq("studio.deploy.status.events"), eq("bundle-1"), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("DEPLOYED");
        assertThat(captor.getValue().errorMessage()).isNull();
    }

    @Test
    void publishesFailedStatus_whenDeployThrows() {
        when(repositoryService.createDeployment()).thenThrow(new RuntimeException("boom"));
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);

        BundleDeployEvent event = new BundleDeployEvent(
                "BUNDLE_DEPLOY_REQUESTED", "bundle-2", "acme", "1.0.0",
                Map.of(), List.of(new BpmnResource("x", "<xml/>")), "alice", Instant.now());

        consumer.consume(event, ack);

        verify(ack).acknowledge();
        ArgumentCaptor<DeploymentStatusEvent> captor = ArgumentCaptor.forClass(DeploymentStatusEvent.class);
        verify(kafkaProducer).send(eq("studio.deploy.status.events"), eq("bundle-2"), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("FAILED");
        assertThat(captor.getValue().errorMessage()).isEqualTo("boom");
    }

    @Test
    void skipsDeployment_whenNoBpmnResources() {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);

        BundleDeployEvent event = new BundleDeployEvent(
                "BUNDLE_DEPLOY_REQUESTED", "bundle-3", "acme", "1.0.0",
                Map.of(), List.of(), "alice", Instant.now());

        consumer.consume(event, ack);

        verify(repositoryService, never()).createDeployment();
        verify(ack).acknowledge();

        ArgumentCaptor<DeploymentStatusEvent> captor = ArgumentCaptor.forClass(DeploymentStatusEvent.class);
        verify(kafkaProducer).send(eq("studio.deploy.status.events"), eq("bundle-3"), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("DEPLOYED");
    }
}
