package com.platform.entitlements.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.entitlements.rbac.domain.Role;
import com.platform.entitlements.rbac.repository.RoleRepository;
import com.platform.entitlements.rbac.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementsCacheService {

    private final RoleRepository roleRepo;
    private final UserRoleRepository userRoleRepo;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * L1: Caffeine cache (5min TTL) for effective permissions per user.
     * Key: "effective-permissions::{tenantId}::{userId}"
     */
    @Cacheable(cacheNames = "effective-permissions", key = "#tenantId + '::' + #userId")
    public Set<String> getEffectivePermissions(String tenantId, String userId) {
        List<UUID> roleIds = userRoleRepo.findByUserIdAndTenantId(userId, tenantId)
                .stream().map(ur -> ur.getRoleId()).toList();

        return roleIds.stream()
                .flatMap(roleId -> roleRepo.findById(roleId).stream())
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet());
    }

    /**
     * L2: Redis (30min TTL) for role → permissions mapping.
     * Key: {tenantId}:role-perms:{roleId}
     */
    public Set<String> getRolePermissionsFromL2(String tenantId, UUID roleId) {
        String key = tenantId + ":role-perms:" + roleId;
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                @SuppressWarnings("unchecked")
                Set<String> perms = objectMapper.readValue(cached, Set.class);
                return perms;
            } catch (Exception e) {
                log.warn("Failed to deserialize L2 role-perms cache", e);
            }
        }
        Role role = roleRepo.findById(roleId).orElse(null);
        if (role == null) return Set.of();
        Set<String> perms = role.getPermissions().stream().map(p -> p.getName()).collect(Collectors.toSet());
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(perms), Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("Failed to write L2 role-perms cache", e);
        }
        return perms;
    }

    /**
     * L2: Redis (60min TTL) for compiled SQL predicate per role+entityType.
     * Key: {tenantId}:sql-predicate:{roleId}:{entityType}
     */
    public String getCompiledPredicate(String tenantId, String roleName, String entityType) {
        String key = tenantId + ":sql-predicate:" + roleName + ":" + entityType;
        return redis.opsForValue().get(key);
    }

    public void cacheCompiledPredicate(String tenantId, String roleName, String entityType, String predicate) {
        String key = tenantId + ":sql-predicate:" + roleName + ":" + entityType;
        redis.opsForValue().set(key, predicate, Duration.ofMinutes(60));
    }

    @CacheEvict(cacheNames = "effective-permissions", key = "#tenantId + '::' + #userId")
    public void evictUserPermissions(String tenantId, String userId) {
        log.debug("Evicted L1 permissions cache for userId={} tenantId={}", userId, tenantId);
    }

    public void evictRolePredicates(String tenantId, String roleName) {
        // Pattern delete from Redis L2
        String pattern = tenantId + ":sql-predicate:" + roleName + ":*";
        var keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
        log.debug("Evicted L2 predicate cache for roleName={} tenantId={}", roleName, tenantId);
    }
}
