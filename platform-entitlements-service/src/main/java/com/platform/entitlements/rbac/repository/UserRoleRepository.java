package com.platform.entitlements.rbac.repository;

import com.platform.entitlements.rbac.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserIdAndTenantId(String userId, String tenantId);
    void deleteByUserIdAndRoleIdAndTenantId(String userId, UUID roleId, String tenantId);
}
