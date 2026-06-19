package com.platform.entitlements.rbac.controller;

import com.platform.common.web.StandardResponseEnvelope;
import com.platform.entitlements.rbac.domain.Role;
import com.platform.entitlements.rbac.service.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    @PostMapping("/roles")
    public ResponseEntity<StandardResponseEnvelope<Role>> createRole(
            @RequestBody Map<String, Object> body, HttpServletRequest http) {
        Role role = rbacService.createRole(
                (String) body.get("name"),
                (String) body.get("displayName"),
                body.get("parentRoleId") != null ? UUID.fromString((String) body.get("parentRoleId")) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ok(role, http));
    }

    @GetMapping("/roles")
    public ResponseEntity<StandardResponseEnvelope<List<Role>>> listRoles(HttpServletRequest http) {
        return ResponseEntity.ok(ok(rbacService.listRoles(), http));
    }

    @PostMapping("/roles/{roleName}/permissions")
    public ResponseEntity<StandardResponseEnvelope<Void>> grantPermission(
            @PathVariable String roleName,
            @RequestBody Map<String, String> body,
            HttpServletRequest http) {
        rbacService.grantPermission(roleName, body.get("permission"));
        return ResponseEntity.ok(ok(null, http));
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<StandardResponseEnvelope<Void>> assignRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> body,
            HttpServletRequest http) {
        rbacService.assignRole(userId, body.get("role"));
        return ResponseEntity.ok(ok(null, http));
    }

    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<StandardResponseEnvelope<Set<String>>> getEffectivePermissions(
            @PathVariable String userId, HttpServletRequest http) {
        return ResponseEntity.ok(ok(rbacService.getEffectivePermissions(userId), http));
    }

    private <T> StandardResponseEnvelope<T> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return StandardResponseEnvelope.of(data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId"));
    }
}
