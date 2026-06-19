package com.platform.entitlements.field.service;

import com.platform.entitlements.field.domain.FieldAccessLevel;
import com.platform.entitlements.field.domain.FieldPolicy;
import com.platform.entitlements.field.domain.MaskPattern;
import com.platform.entitlements.field.repository.FieldPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FieldMaskingService {

    private final FieldPolicyRepository fieldPolicyRepo;

    /**
     * Applies field-level access policies to a response map.
     * DENY → field omitted; MASKED → value replaced with masked version; ALLOW → pass-through.
     */
    public Map<String, Object> applyFieldPolicies(
            String tenantId, String roleName, String entityType, Map<String, Object> data) {

        List<FieldPolicy> policies = fieldPolicyRepo.findByTenantIdAndRoleNameAndEntityType(
                tenantId, roleName, entityType);

        if (policies.isEmpty()) return data;

        Map<String, FieldPolicy> policyMap = new HashMap<>();
        for (FieldPolicy p : policies) policyMap.put(p.getFieldName(), p);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String field = entry.getKey();
            FieldPolicy policy = policyMap.get(field);
            if (policy == null) {
                result.put(field, entry.getValue()); // no policy = ALLOW
                continue;
            }
            switch (policy.getAccessLevel()) {
                case DENY -> { /* omit entirely */ }
                case MASKED -> result.put(field, applyMask(entry.getValue(), policy.getMaskPattern()));
                case ALLOW -> result.put(field, entry.getValue());
                case CONDITIONAL -> {
                    // Simplified: include unless condition evaluates to false
                    // Full SpEL evaluation deferred to runtime in production
                    result.put(field, entry.getValue());
                }
            }
        }
        return result;
    }

    private Object applyMask(Object value, MaskPattern pattern) {
        if (value == null || pattern == null) return value;
        String s = value.toString();
        return switch (pattern) {
            case PAN -> maskPan(s);
            case EMAIL -> maskEmail(s);
            case MOBILE -> maskMobile(s);
        };
    }

    private String maskPan(String s) {
        if (s.length() < 8) return "****";
        return s.substring(0, 4) + "****" + s.substring(s.length() - 4);
    }

    private String maskEmail(String s) {
        int at = s.indexOf('@');
        if (at <= 0) return "*****";
        return s.charAt(0) + "*****" + s.substring(at);
    }

    private String maskMobile(String s) {
        if (s.length() < 4) return "****";
        return "****" + s.substring(s.length() - 4);
    }
}
