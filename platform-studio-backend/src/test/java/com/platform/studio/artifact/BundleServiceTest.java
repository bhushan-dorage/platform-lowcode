package com.platform.studio.artifact;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.studio.artifact.domain.ArtifactStatus;
import com.platform.studio.artifact.domain.ArtifactType;
import com.platform.studio.artifact.domain.BundleStatus;
import com.platform.studio.artifact.domain.DeploymentBundle;
import com.platform.studio.artifact.dto.ArtifactContentDto;
import com.platform.studio.artifact.dto.ArtifactDto;
import com.platform.studio.artifact.dto.CreateBundleRequest;
import com.platform.studio.artifact.messaging.BundleDeployEvent;
import com.platform.studio.artifact.repository.DeploymentBundleRepository;
import com.platform.studio.artifact.service.ArtifactService;
import com.platform.studio.artifact.service.BundleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BundleServiceTest {

    @Mock DeploymentBundleRepository bundleRepo;
    @Mock ArtifactService artifactService;
    @Mock TenantAwareKafkaProducer kafkaProducer;
    @InjectMocks BundleService bundleService;

    @BeforeEach
    void setup() {
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private ArtifactDto artifactDto(UUID id, ArtifactType type, String name) {
        return new ArtifactDto(id, "acme", type, name, name, null, "1.0.0", "sha",
                ArtifactStatus.PUBLISHED, "alice", Instant.now(), Instant.now(), Instant.now());
    }

    @Test
    void deployBundle_partitionsBpmnAndDmnResourcesInEvent() {
        UUID bundleId = UUID.randomUUID();
        UUID bpmnArtifactId = UUID.randomUUID();
        UUID dmnArtifactId = UUID.randomUUID();
        UUID formArtifactId = UUID.randomUUID();

        DeploymentBundle bundle = new DeploymentBundle();
        bundle.setId(bundleId);
        bundle.setTenantId("acme");
        bundle.setVersion("1.0.0");
        bundle.setStatus(BundleStatus.DRAFT);
        bundle.setArtifactVersions(Map.of(
                bpmnArtifactId.toString(), "1.0.0",
                dmnArtifactId.toString(), "1.0.0",
                formArtifactId.toString(), "2.0.0"
        ));

        when(bundleRepo.findByIdAndTenantId(bundleId, "acme")).thenReturn(Optional.of(bundle));
        when(bundleRepo.save(any())).thenReturn(bundle);
        when(artifactService.getPublishedContent(bpmnArtifactId, "1.0.0"))
                .thenReturn(new ArtifactContentDto(artifactDto(bpmnArtifactId, ArtifactType.BPMN, "loan-approval"), "<bpmn/>"));
        when(artifactService.getPublishedContent(dmnArtifactId, "1.0.0"))
                .thenReturn(new ArtifactContentDto(artifactDto(dmnArtifactId, ArtifactType.DMN, "loan-eligibility"), "<dmn/>"));
        when(artifactService.getPublishedContent(formArtifactId, "2.0.0"))
                .thenReturn(new ArtifactContentDto(artifactDto(formArtifactId, ArtifactType.FORM, "intake-form"), "{}"));

        bundleService.deployBundle(bundleId, "alice");

        assertThat(bundle.getStatus()).isEqualTo(BundleStatus.DEPLOYING);

        ArgumentCaptor<BundleDeployEvent> captor = ArgumentCaptor.forClass(BundleDeployEvent.class);
        verify(kafkaProducer).send(eq("studio.deploy.events"), eq(bundleId.toString()), captor.capture());

        BundleDeployEvent event = captor.getValue();
        assertThat(event.tenantId()).isEqualTo("acme");
        assertThat(event.bundleId()).isEqualTo(bundleId.toString());
        assertThat(event.resources()).hasSize(1);
        assertThat(event.resources().get(0).name()).isEqualTo("loan-approval");
        assertThat(event.resources().get(0).content()).isEqualTo("<bpmn/>");
        assertThat(event.dmnResources()).hasSize(1);
        assertThat(event.dmnResources().get(0).name()).isEqualTo("loan-eligibility");
        assertThat(event.dmnResources().get(0).content()).isEqualTo("<dmn/>");
    }

    @Test
    void deployBundle_alreadyDeploying_throwsIllegalState() {
        UUID bundleId = UUID.randomUUID();
        DeploymentBundle bundle = new DeploymentBundle();
        bundle.setId(bundleId);
        bundle.setTenantId("acme");
        bundle.setStatus(BundleStatus.DEPLOYING);

        when(bundleRepo.findByIdAndTenantId(bundleId, "acme")).thenReturn(Optional.of(bundle));

        assertThatThrownBy(() -> bundleService.deployBundle(bundleId, "alice"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void createBundle_duplicateVersion_throwsIllegalArgument() {
        when(bundleRepo.existsByTenantIdAndVersion("acme", "1.0.0")).thenReturn(true);

        assertThatThrownBy(() -> bundleService.createBundle(
                new CreateBundleRequest("1.0.0", Map.of()), "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
