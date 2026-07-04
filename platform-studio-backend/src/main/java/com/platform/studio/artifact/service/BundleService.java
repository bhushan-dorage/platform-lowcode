package com.platform.studio.artifact.service;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.studio.artifact.domain.ArtifactType;
import com.platform.studio.artifact.domain.BundleStatus;
import com.platform.studio.artifact.domain.DeploymentBundle;
import com.platform.studio.artifact.dto.CreateBundleRequest;
import com.platform.studio.artifact.messaging.BpmnResource;
import com.platform.studio.artifact.messaging.BundleDeployEvent;
import com.platform.studio.artifact.repository.DeploymentBundleRepository;
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
public class BundleService {

    private final DeploymentBundleRepository bundleRepo;
    private final ArtifactService artifactService;
    private final TenantAwareKafkaProducer kafkaProducer;

    @Timed(value = "studio.bundle.create")
    @Transactional
    public DeploymentBundle createBundle(CreateBundleRequest req, String userId) {
        String tenantId = TenantContext.getTenantId();
        if (bundleRepo.existsByTenantIdAndVersion(tenantId, req.version())) {
            throw new IllegalArgumentException("Bundle version already exists: " + req.version());
        }
        DeploymentBundle bundle = new DeploymentBundle();
        bundle.setTenantId(tenantId);
        bundle.setVersion(req.version());
        bundle.setArtifactVersions(req.artifactVersions());
        bundle.setCreatedBy(userId);
        return bundleRepo.save(bundle);
    }

    @Timed(value = "studio.bundle.deploy")
    @Transactional
    public DeploymentBundle deployBundle(UUID bundleId, String userId) {
        String tenantId = TenantContext.getTenantId();
        DeploymentBundle bundle = findOrThrow(bundleId, tenantId);

        if (bundle.getStatus() == BundleStatus.DEPLOYING || bundle.getStatus() == BundleStatus.DEPLOYED) {
            throw new IllegalStateException("Bundle is already " + bundle.getStatus());
        }

        bundle.setStatus(BundleStatus.DEPLOYING);
        bundleRepo.save(bundle);

        List<BpmnResource> bpmnResources = bundle.getArtifactVersions().entrySet().stream()
                .map(e -> artifactService.getPublishedContent(UUID.fromString(e.getKey()), e.getValue()))
                .filter(content -> content.metadata().type() == ArtifactType.BPMN)
                .map(content -> new BpmnResource(content.metadata().name(), content.content()))
                .toList();

        kafkaProducer.send("studio.deploy.events", bundleId.toString(), new BundleDeployEvent(
                "BUNDLE_DEPLOY_REQUESTED",
                bundleId.toString(),
                tenantId,
                bundle.getVersion(),
                bundle.getArtifactVersions(),
                bpmnResources,
                userId,
                Instant.now()
        ));
        log.info("Bundle deploy requested bundleId={} version={} bpmnResourceCount={} by={}",
                bundleId, bundle.getVersion(), bpmnResources.size(), userId);
        return bundle;
    }

    public DeploymentBundle getBundle(UUID bundleId) {
        return findOrThrow(bundleId, TenantContext.getTenantId());
    }

    public List<DeploymentBundle> listBundles() {
        return bundleRepo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    private DeploymentBundle findOrThrow(UUID id, String tenantId) {
        return bundleRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Bundle not found: " + id));
    }
}
