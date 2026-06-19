package com.platform.entitlements.abac.repository;

import com.platform.entitlements.abac.domain.DataPolicy;
import com.platform.entitlements.abac.domain.PolicyOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataPolicyRepository extends JpaRepository<DataPolicy, UUID> {
    Optional<DataPolicy> findByTenantIdAndRoleNameAndEntityTypeAndOperation(
            String tenantId, String roleName, String entityType, PolicyOperation operation);
    List<DataPolicy> findByTenantIdAndRoleName(String tenantId, String roleName);
    List<DataPolicy> findByTenantIdAndRoleNameAndEntityType(String tenantId, String roleName, String entityType);
    List<DataPolicy> findByTenantId(String tenantId);
    Optional<DataPolicy> findByIdAndTenantId(UUID id, String tenantId);
}
