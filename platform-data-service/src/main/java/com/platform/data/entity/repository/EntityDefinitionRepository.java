package com.platform.data.entity.repository;

import com.platform.data.entity.domain.EntityDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityDefinitionRepository extends JpaRepository<EntityDefinition, UUID> {
    Optional<EntityDefinition> findByTenantIdAndEntityTypeAndArchivedFalse(String tenantId, String entityType);
    List<EntityDefinition> findByTenantIdAndArchivedFalse(String tenantId);
    boolean existsByTenantIdAndEntityType(String tenantId, String entityType);
}
