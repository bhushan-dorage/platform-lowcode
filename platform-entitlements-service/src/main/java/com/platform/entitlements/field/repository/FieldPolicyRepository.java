package com.platform.entitlements.field.repository;

import com.platform.entitlements.field.domain.FieldPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FieldPolicyRepository extends JpaRepository<FieldPolicy, UUID> {
    List<FieldPolicy> findByTenantIdAndRoleNameAndEntityType(String tenantId, String roleName, String entityType);
}
