package com.platform.entitlements.abac.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.platform.entitlements.abac.domain.DataPolicy;
import com.platform.entitlements.abac.repository.DataPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Compiles YAML data policies to parameterized SQL predicates at activation time.
 * Compiled predicate is stored in data_policies.compiled_predicate — never compiled at query time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyCompilerService {

    private final DataPolicyRepository policyRepo;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Transactional
    public String compile(DataPolicy policy) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = yamlMapper.readValue(policy.getPolicyYaml(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) parsed.getOrDefault("conditions", List.of());

            String predicate = conditions.stream()
                    .map(this::compileCondition)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(" AND "));

            policy.setCompiledPredicate(predicate);
            policyRepo.save(policy);
            log.info("Compiled policy id={} predicate={}", policy.getId(), predicate);
            return predicate;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to compile policy: " + e.getMessage(), e);
        }
    }

    private String compileCondition(Map<String, Object> cond) {
        String field = toSnakeCase((String) cond.get("field"));
        String op = (String) cond.get("op");
        Object value = cond.get("value");
        Object values = cond.get("values");

        return switch (op) {
            case "EQUALS" -> field + " = " + toParam(value);
            case "NOT_EQUALS" -> field + " != " + toParam(value);
            case "LESS_THAN" -> field + " < " + toParam(value);
            case "LESS_THAN_OR_EQUAL" -> field + " <= " + toParam(value);
            case "GREATER_THAN" -> field + " > " + toParam(value);
            case "GREATER_THAN_OR_EQUAL" -> field + " >= " + toParam(value);
            case "IN" -> field + " IN (" + toInList(values) + ")";
            case "NOT_IN" -> field + " NOT IN (" + toInList(values) + ")";
            case "LIKE" -> field + " LIKE " + toParam(value);
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }

    private String toParam(Object value) {
        if (value == null) return "NULL";
        String v = value.toString();
        // Actor attribute reference: ${actor.attributes.fieldName} → named param :actorFieldName
        if (v.startsWith("${actor.attributes.")) {
            String attr = v.substring("${actor.attributes.".length(), v.length() - 1);
            return ":actor" + capitalize(attr);
        }
        // String literal
        return "'" + v.replace("'", "''") + "'";
    }

    @SuppressWarnings("unchecked")
    private String toInList(Object values) {
        if (values instanceof List<?> list) {
            return list.stream().map(v -> "'" + v.toString().replace("'", "''") + "'")
                    .collect(Collectors.joining(", "));
        }
        return "";
    }

    private static String toSnakeCase(String camel) {
        return camel.replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("^_", "");
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
