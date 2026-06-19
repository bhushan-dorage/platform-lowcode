package com.platform.entitlements.abac.controller;

import com.platform.common.tenant.TenantContext;
import com.platform.common.web.StandardResponseEnvelope;
import com.platform.entitlements.abac.domain.DataPolicy;
import com.platform.entitlements.abac.domain.PolicyOperation;
import com.platform.entitlements.abac.repository.DataPolicyRepository;
import com.platform.entitlements.abac.service.PolicyCompilerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/abac")
@RequiredArgsConstructor
public class DataPolicyController {

    private final DataPolicyRepository policyRepo;
    private final PolicyCompilerService compilerService;

    @PostMapping("/policies")
    public ResponseEntity<StandardResponseEnvelope<DataPolicy>> createPolicy(
            @RequestBody Map<String, Object> body, HttpServletRequest http) {
        String tenantId = TenantContext.getTenantId();
        DataPolicy policy = new DataPolicy();
        policy.setTenantId(tenantId);
        policy.setRoleName((String) body.get("roleName"));
        policy.setEntityType((String) body.get("entityType"));
        policy.setOperation(PolicyOperation.valueOf((String) body.get("operation")));
        policy.setPolicyYaml((String) body.get("policyYaml"));
        compilerService.compile(policy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ok(policy, http));
    }

    @GetMapping("/policies")
    public ResponseEntity<StandardResponseEnvelope<List<DataPolicy>>> listPolicies(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String entityType,
            HttpServletRequest http) {
        String tenantId = TenantContext.getTenantId();
        List<DataPolicy> policies;
        if (roleName != null && entityType != null) {
            policies = policyRepo.findByTenantIdAndRoleNameAndEntityType(tenantId, roleName, entityType);
        } else if (roleName != null) {
            policies = policyRepo.findByTenantIdAndRoleName(tenantId, roleName);
        } else {
            policies = policyRepo.findByTenantId(tenantId);
        }
        return ResponseEntity.ok(ok(policies, http));
    }

    @GetMapping("/policies/{policyId}")
    public ResponseEntity<StandardResponseEnvelope<DataPolicy>> getPolicy(
            @PathVariable UUID policyId, HttpServletRequest http) {
        DataPolicy policy = policyRepo.findByIdAndTenantId(policyId, TenantContext.getTenantId())
                .orElseThrow(() -> new com.platform.entitlements.exception.ResourceNotFoundException("Policy not found: " + policyId));
        return ResponseEntity.ok(ok(policy, http));
    }

    @PutMapping("/policies/{policyId}")
    public ResponseEntity<StandardResponseEnvelope<DataPolicy>> updatePolicy(
            @PathVariable UUID policyId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest http) {
        DataPolicy policy = policyRepo.findByIdAndTenantId(policyId, TenantContext.getTenantId())
                .orElseThrow(() -> new com.platform.entitlements.exception.ResourceNotFoundException("Policy not found: " + policyId));
        if (body.containsKey("policyYaml")) {
            policy.setPolicyYaml((String) body.get("policyYaml"));
            compilerService.compile(policy);
        }
        return ResponseEntity.ok(ok(policy, http));
    }

    @DeleteMapping("/policies/{policyId}")
    public ResponseEntity<StandardResponseEnvelope<Void>> deletePolicy(
            @PathVariable UUID policyId, HttpServletRequest http) {
        DataPolicy policy = policyRepo.findByIdAndTenantId(policyId, TenantContext.getTenantId())
                .orElseThrow(() -> new com.platform.entitlements.exception.ResourceNotFoundException("Policy not found: " + policyId));
        policyRepo.delete(policy);
        return ResponseEntity.ok(ok(null, http));
    }

    private <T> StandardResponseEnvelope<T> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return StandardResponseEnvelope.of(data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId"));
    }
}
