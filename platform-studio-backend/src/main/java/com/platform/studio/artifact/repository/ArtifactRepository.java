package com.platform.studio.artifact.repository;

import com.platform.studio.artifact.domain.Artifact;
import com.platform.studio.artifact.domain.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {
    Optional<Artifact> findByTenantIdAndTypeAndName(String tenantId, ArtifactType type, String name);
    Optional<Artifact> findByIdAndTenantId(UUID id, String tenantId);
    List<Artifact> findByTenantId(String tenantId);
    List<Artifact> findByTenantIdAndType(String tenantId, ArtifactType type);
    boolean existsByTenantIdAndTypeAndName(String tenantId, ArtifactType type, String name);
}
