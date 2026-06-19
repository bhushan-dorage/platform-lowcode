package com.platform.entitlements.rbac.repository;

import com.platform.entitlements.rbac.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByTenantIdAndName(String tenantId, String name);
    List<Role> findByTenantId(String tenantId);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE r.tenantId = :tenantId AND r.name = :roleName")
    Optional<Role> findByTenantIdAndNameWithPermissions(String tenantId, String roleName);
}
