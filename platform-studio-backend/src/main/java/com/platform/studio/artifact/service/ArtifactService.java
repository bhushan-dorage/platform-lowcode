package com.platform.studio.artifact.service;

import com.platform.common.tenant.TenantContext;
import com.platform.studio.artifact.bridge.DataServiceClient;
import com.platform.studio.artifact.bridge.FormFieldsToJsonSchemaMapper;
import com.platform.studio.artifact.bridge.FormPublishPayload;
import com.platform.studio.artifact.bridge.FormServiceClient;
import com.platform.studio.artifact.domain.Artifact;
import com.platform.studio.artifact.domain.ArtifactStatus;
import com.platform.studio.artifact.domain.ArtifactType;
import com.platform.studio.artifact.dto.ArtifactContentDto;
import com.platform.studio.artifact.dto.ArtifactDto;
import com.platform.studio.artifact.dto.SaveArtifactRequest;
import com.platform.studio.artifact.repository.ArtifactRepository;
import com.platform.studio.exception.ResourceNotFoundException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final ArtifactRepository artifactRepo;
    private final GitArtifactStore gitStore;
    private final FormFieldsToJsonSchemaMapper formFieldsMapper;
    private final FormServiceClient formServiceClient;
    private final DataServiceClient dataServiceClient;

    @Timed(value = "studio.artifact.save")
    @Transactional
    public ArtifactDto save(SaveArtifactRequest req, String userId) {
        String tenantId = TenantContext.getTenantId();
        Artifact artifact = artifactRepo
                .findByTenantIdAndTypeAndName(tenantId, req.type(), req.name())
                .orElseGet(() -> {
                    Artifact a = new Artifact();
                    a.setTenantId(tenantId);
                    a.setType(req.type());
                    a.setName(req.name());
                    a.setCreatedBy(userId);
                    return a;
                });

        artifact.setDisplayName(req.displayName());
        artifact.setDescription(req.description());

        String sha = gitStore.commit(tenantId, req.type(), req.name(), req.content(), userId);
        artifact.setHeadCommitSha(sha);
        return ArtifactDto.from(artifactRepo.save(artifact));
    }

    @Timed(value = "studio.artifact.publish")
    @Transactional
    public ArtifactDto publish(UUID artifactId, String version, String userId) {
        String tenantId = TenantContext.getTenantId();
        Artifact artifact = findOrThrow(artifactId, tenantId);

        if (artifact.getHeadCommitSha() == null) {
            throw new IllegalStateException("Artifact has no committed content");
        }
        gitStore.tag(tenantId, artifact.getType(), artifact.getName(), version, artifact.getHeadCommitSha());
        bridgeToRuntimeService(artifact);

        artifact.setCurrentVersion(version);
        artifact.setStatus(ArtifactStatus.PUBLISHED);
        artifact.setPublishedAt(Instant.now());
        log.info("Published artifact id={} version={} by userId={}", artifactId, version, userId);
        return ArtifactDto.from(artifactRepo.save(artifact));
    }

    /**
     * Pushes FORM/DATA_MODEL artifacts into their runtime service's own persistence so what
     * Studio publishes is actually what the Portal serves. BPMN goes through the bundle-deploy
     * path instead; DMN/RULE_SET are out of scope (rules-service isn't wired up yet).
     */
    private void bridgeToRuntimeService(Artifact artifact) {
        if (artifact.getType() != ArtifactType.FORM && artifact.getType() != ArtifactType.DATA_MODEL) {
            return; // BPMN handled by bundle deploy; DMN/RULE_SET out of scope
        }
        String displayName = artifact.getDisplayName() != null ? artifact.getDisplayName() : artifact.getName();
        String content = gitStore.readContent(
                artifact.getTenantId(), artifact.getType(), artifact.getName(), artifact.getHeadCommitSha());

        if (artifact.getType() == ArtifactType.FORM) {
            FormPublishPayload payload = formFieldsMapper.map(content);
            formServiceClient.publish(artifact.getName(), displayName, payload);
        } else {
            dataServiceClient.publish(artifact.getName(), displayName, content);
        }
    }

    @Timed(value = "studio.artifact.get-content")
    public ArtifactContentDto getContent(UUID artifactId, String ref) {
        String tenantId = TenantContext.getTenantId();
        Artifact artifact = findOrThrow(artifactId, tenantId);
        String resolvedRef = ref != null ? ref
                : (artifact.getHeadCommitSha() != null ? artifact.getHeadCommitSha() : "HEAD");
        String content = gitStore.readContent(tenantId, artifact.getType(), artifact.getName(), resolvedRef);
        return new ArtifactContentDto(ArtifactDto.from(artifact), content);
    }

    @Timed(value = "studio.artifact.get-published-content")
    public ArtifactContentDto getPublishedContent(UUID artifactId, String version) {
        String tenantId = TenantContext.getTenantId();
        Artifact artifact = findOrThrow(artifactId, tenantId);
        String tag = gitStore.tagName(artifact.getType(), artifact.getName(), version);
        String content = gitStore.readContent(tenantId, artifact.getType(), artifact.getName(), tag);
        if (content == null) {
            throw new ResourceNotFoundException("Version " + version + " not found for artifact " + artifactId);
        }
        return new ArtifactContentDto(ArtifactDto.from(artifact), content);
    }

    public List<ArtifactDto> list(ArtifactType type) {
        String tenantId = TenantContext.getTenantId();
        List<Artifact> artifacts = type != null
                ? artifactRepo.findByTenantIdAndType(tenantId, type)
                : artifactRepo.findByTenantId(tenantId);
        return artifacts.stream().map(ArtifactDto::from).toList();
    }

    @Transactional
    public ArtifactDto deprecate(UUID artifactId) {
        Artifact artifact = findOrThrow(artifactId, TenantContext.getTenantId());
        artifact.setStatus(ArtifactStatus.DEPRECATED);
        return ArtifactDto.from(artifactRepo.save(artifact));
    }

    private Artifact findOrThrow(UUID id, String tenantId) {
        return artifactRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + id));
    }
}
