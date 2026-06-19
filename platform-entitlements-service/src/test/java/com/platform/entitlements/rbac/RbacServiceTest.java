package com.platform.entitlements.rbac;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.entitlements.cache.EntitlementsCacheService;
import com.platform.entitlements.rbac.domain.Permission;
import com.platform.entitlements.rbac.domain.Role;
import com.platform.entitlements.rbac.domain.UserRole;
import com.platform.entitlements.rbac.repository.PermissionRepository;
import com.platform.entitlements.rbac.repository.RoleRepository;
import com.platform.entitlements.rbac.repository.UserRoleRepository;
import com.platform.entitlements.rbac.service.RbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock RoleRepository roleRepo;
    @Mock PermissionRepository permissionRepo;
    @Mock UserRoleRepository userRoleRepo;
    @Mock EntitlementsCacheService cacheService;

    @InjectMocks RbacService rbacService;

    @BeforeEach
    void setup() {
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
    }

    @Test
    void createRole_persistsWithTenantId() {
        Role saved = new Role();
        saved.setId(UUID.randomUUID());
        saved.setTenantId("acme");
        saved.setName("analyst");
        when(roleRepo.save(any(Role.class))).thenReturn(saved);

        Role result = rbacService.createRole("analyst", "Analyst", null);

        assertThat(result.getTenantId()).isEqualTo("acme");
        assertThat(result.getName()).isEqualTo("analyst");
        verify(roleRepo).save(any(Role.class));
    }

    @Test
    void grantPermission_createsPermissionIfAbsent() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setTenantId("acme");
        role.setName("analyst");
        role.setPermissions(new HashSet<>());

        Permission perm = new Permission();
        perm.setId(UUID.randomUUID());
        perm.setName("platform:reports:read");

        when(roleRepo.findByTenantIdAndName("acme", "analyst")).thenReturn(Optional.of(role));
        when(permissionRepo.findByName("platform:reports:read")).thenReturn(Optional.empty());
        when(permissionRepo.save(any(Permission.class))).thenReturn(perm);
        when(roleRepo.save(any(Role.class))).thenReturn(role);

        rbacService.grantPermission("analyst", "platform:reports:read");

        assertThat(role.getPermissions()).hasSize(1);
        verify(cacheService).evictRolePredicates("acme", "analyst");
    }

    @Test
    void grantPermission_throwsWhenRoleNotFound() {
        when(roleRepo.findByTenantIdAndName("acme", "unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rbacService.grantPermission("unknown", "platform:x:y"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void assignRole_savesUserRoleAndEvictsCache() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setTenantId("acme");
        role.setName("analyst");
        role.setPermissions(new HashSet<>());

        when(roleRepo.findByTenantIdAndName("acme", "analyst")).thenReturn(Optional.of(role));
        when(userRoleRepo.save(any(UserRole.class))).thenAnswer(i -> i.getArgument(0));

        rbacService.assignRole("user-123", "analyst");

        verify(userRoleRepo).save(any(UserRole.class));
        verify(cacheService).evictUserPermissions("acme", "user-123");
    }

    @Test
    void getEffectivePermissions_delegatesToCacheService() {
        when(cacheService.getEffectivePermissions("acme", "user-123"))
                .thenReturn(java.util.Set.of("platform:reports:read"));

        var result = rbacService.getEffectivePermissions("user-123");

        assertThat(result).containsExactly("platform:reports:read");
    }
}
