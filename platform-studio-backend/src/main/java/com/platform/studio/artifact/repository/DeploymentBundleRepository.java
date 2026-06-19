package com.platform.studio.artifact.repository;

import com.platform.studio.artifact.domain.DeploymentBundle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentBundleRepository extends JpaRepository<DeploymentBundle, UUID> {
    List<DeploymentBundle> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<DeploymentBundle> findByIdAndTenantId(UUID id, String tenantId);
    boolean existsByTenantIdAndVersion(String tenantId, String version);
}
