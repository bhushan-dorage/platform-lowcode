package com.platform.studio.artifact;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.studio.artifact.bridge.DataServiceClient;
import com.platform.studio.artifact.bridge.FormFieldsToJsonSchemaMapper;
import com.platform.studio.artifact.bridge.FormPublishPayload;
import com.platform.studio.artifact.bridge.FormServiceClient;
import com.platform.studio.artifact.domain.Artifact;
import com.platform.studio.artifact.domain.ArtifactStatus;
import com.platform.studio.artifact.domain.ArtifactType;
import com.platform.studio.artifact.dto.ArtifactDto;
import com.platform.studio.artifact.dto.SaveArtifactRequest;
import com.platform.studio.artifact.repository.ArtifactRepository;
import com.platform.studio.artifact.service.ArtifactService;
import com.platform.studio.artifact.service.GitArtifactStore;
import com.platform.studio.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceTest {

    @Mock ArtifactRepository artifactRepo;
    @Mock GitArtifactStore gitStore;
    @Mock FormFieldsToJsonSchemaMapper formFieldsMapper;
    @Mock FormServiceClient formServiceClient;
    @Mock DataServiceClient dataServiceClient;
    @InjectMocks ArtifactService artifactService;

    @BeforeEach
    void setup() {
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
    }

    @Test
    void save_createsNewArtifactWithCommitSha() {
        when(artifactRepo.findByTenantIdAndTypeAndName("acme", ArtifactType.BPMN, "invoice-approval"))
                .thenReturn(Optional.empty());
        when(gitStore.commit(eq("acme"), eq(ArtifactType.BPMN), eq("invoice-approval"), any(), eq("alice")))
                .thenReturn("abc123sha");
        Artifact saved = new Artifact();
        saved.setId(UUID.randomUUID());
        saved.setTenantId("acme");
        saved.setType(ArtifactType.BPMN);
        saved.setName("invoice-approval");
        saved.setHeadCommitSha("abc123sha");
        saved.setStatus(ArtifactStatus.DRAFT);
        when(artifactRepo.save(any())).thenReturn(saved);

        ArtifactDto dto = artifactService.save(
                new SaveArtifactRequest(ArtifactType.BPMN, "invoice-approval", "Invoice Approval", null, "<bpmn/>"),
                "alice");

        assertThat(dto.headCommitSha()).isEqualTo("abc123sha");
        assertThat(dto.status()).isEqualTo(ArtifactStatus.DRAFT);
    }

    @Test
    void save_existingArtifactUpdatesContent() {
        Artifact existing = new Artifact();
        existing.setId(UUID.randomUUID());
        existing.setTenantId("acme");
        existing.setType(ArtifactType.BPMN);
        existing.setName("invoice-approval");
        existing.setStatus(ArtifactStatus.DRAFT);

        when(artifactRepo.findByTenantIdAndTypeAndName("acme", ArtifactType.BPMN, "invoice-approval"))
                .thenReturn(Optional.of(existing));
        when(gitStore.commit(any(), any(), any(), any(), any())).thenReturn("newsha");
        when(artifactRepo.save(any())).thenReturn(existing);

        artifactService.save(
                new SaveArtifactRequest(ArtifactType.BPMN, "invoice-approval", "Updated Name", null, "<bpmn>v2</bpmn>"),
                "alice");

        verify(gitStore).commit("acme", ArtifactType.BPMN, "invoice-approval", "<bpmn>v2</bpmn>", "alice");
    }

    @Test
    void publish_tagsBoundToCommitSha() {
        UUID id = UUID.randomUUID();
        Artifact artifact = new Artifact();
        artifact.setId(id);
        artifact.setTenantId("acme");
        artifact.setType(ArtifactType.BPMN);
        artifact.setName("invoice-approval");
        artifact.setHeadCommitSha("sha123");
        artifact.setStatus(ArtifactStatus.DRAFT);

        when(artifactRepo.findByIdAndTenantId(id, "acme")).thenReturn(Optional.of(artifact));
        when(artifactRepo.save(any())).thenReturn(artifact);

        artifactService.publish(id, "1.0.0", "alice");

        verify(gitStore).tag("acme", ArtifactType.BPMN, "invoice-approval", "1.0.0", "sha123");
        assertThat(artifact.getStatus()).isEqualTo(ArtifactStatus.PUBLISHED);
        assertThat(artifact.getCurrentVersion()).isEqualTo("1.0.0");
        verifyNoInteractions(formServiceClient, dataServiceClient);
    }

    @Test
    void publish_formArtifact_bridgesToFormService() {
        UUID id = UUID.randomUUID();
        Artifact artifact = new Artifact();
        artifact.setId(id);
        artifact.setTenantId("acme");
        artifact.setType(ArtifactType.FORM);
        artifact.setName("intake-form");
        artifact.setDisplayName("Intake Form");
        artifact.setHeadCommitSha("sha123");
        artifact.setStatus(ArtifactStatus.DRAFT);

        when(artifactRepo.findByIdAndTenantId(id, "acme")).thenReturn(Optional.of(artifact));
        when(artifactRepo.save(any())).thenReturn(artifact);
        when(gitStore.readContent("acme", ArtifactType.FORM, "intake-form", "sha123"))
                .thenReturn("{\"formKey\":\"intake-form\",\"fields\":[]}");
        FormPublishPayload payload = new FormPublishPayload("{}", "[]");
        when(formFieldsMapper.map(any())).thenReturn(payload);

        artifactService.publish(id, "1.0.0", "alice");

        verify(formServiceClient).publish("intake-form", "Intake Form", payload);
        verifyNoInteractions(dataServiceClient);
    }

    @Test
    void publish_dataModelArtifact_bridgesToDataService() {
        UUID id = UUID.randomUUID();
        Artifact artifact = new Artifact();
        artifact.setId(id);
        artifact.setTenantId("acme");
        artifact.setType(ArtifactType.DATA_MODEL);
        artifact.setName("invoice");
        artifact.setDisplayName("Invoice");
        artifact.setHeadCommitSha("sha456");
        artifact.setStatus(ArtifactStatus.DRAFT);

        when(artifactRepo.findByIdAndTenantId(id, "acme")).thenReturn(Optional.of(artifact));
        when(artifactRepo.save(any())).thenReturn(artifact);
        when(gitStore.readContent("acme", ArtifactType.DATA_MODEL, "invoice", "sha456"))
                .thenReturn("{\"type\":\"object\",\"properties\":{}}");

        artifactService.publish(id, "1.0.0", "alice");

        verify(dataServiceClient).publish("invoice", "Invoice", "{\"type\":\"object\",\"properties\":{}}");
        verifyNoInteractions(formServiceClient);
    }

    @Test
    void publish_withNoContent_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        Artifact artifact = new Artifact();
        artifact.setId(id);
        artifact.setTenantId("acme");
        artifact.setType(ArtifactType.BPMN);
        artifact.setName("empty");
        artifact.setHeadCommitSha(null);

        when(artifactRepo.findByIdAndTenantId(id, "acme")).thenReturn(Optional.of(artifact));

        assertThatThrownBy(() -> artifactService.publish(id, "1.0.0", "alice"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getContent_unknownArtifact_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(artifactRepo.findByIdAndTenantId(id, "acme")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artifactService.getContent(id, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
