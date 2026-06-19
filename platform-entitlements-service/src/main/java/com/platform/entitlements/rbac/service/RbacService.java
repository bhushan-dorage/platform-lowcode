package com.platform.entitlements.rbac.service;

import com.platform.common.tenant.TenantContext;
import com.platform.entitlements.cache.EntitlementsCacheService;
import com.platform.entitlements.rbac.domain.Permission;
import com.platform.entitlements.rbac.domain.Role;
import com.platform.entitlements.rbac.domain.UserRole;
import com.platform.entitlements.rbac.repository.PermissionRepository;
import com.platform.entitlements.rbac.repository.RoleRepository;
import com.platform.entitlements.rbac.repository.UserRoleRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permissionRepo;
    private final UserRoleRepository userRoleRepo;
    private final EntitlementsCacheService cacheService;

    @Timed(name = "entitlements.role.create")
    @Transactional
    public Role createRole(String name, String displayName, UUID parentRoleId) {
        String tenantId = TenantContext.getTenantId();
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName(name);
        role.setDisplayName(displayName);
        role.setParentRoleId(parentRoleId);
        return roleRepo.save(role);
    }

    @Timed(name = "entitlements.role.grant-permission")
    @Transactional
    public void grantPermission(String roleName, String permissionName) {
        String tenantId = TenantContext.getTenantId();
        Role role = roleRepo.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        Permission perm = permissionRepo.findByName(permissionName)
                .orElseGet(() -> {
                    Permission p = new Permission();
                    p.setName(permissionName);
                    return permissionRepo.save(p);
                });
        role.getPermissions().add(perm);
        roleRepo.save(role);
        // Invalidate L2 cache for this role
        cacheService.evictRolePredicates(tenantId, roleName);
    }

    @Timed(name = "entitlements.user.assign-role")
    @Transactional
    public void assignRole(String userId, String roleName) {
        String tenantId = TenantContext.getTenantId();
        Role role = roleRepo.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        ur.setTenantId(tenantId);
        userRoleRepo.save(ur);
        cacheService.evictUserPermissions(tenantId, userId);
    }

    @Timed(name = "entitlements.user.effective-permissions")
    public Set<String> getEffectivePermissions(String userId) {
        return cacheService.getEffectivePermissions(TenantContext.getTenantId(), userId);
    }

    public List<Role> listRoles() {
        return roleRepo.findByTenantId(TenantContext.getTenantId());
    }
}
