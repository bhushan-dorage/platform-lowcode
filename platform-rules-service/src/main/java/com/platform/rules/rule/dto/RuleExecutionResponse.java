package com.platform.rules.rule.dto;

import java.util.List;
import java.util.Map;

public record RuleExecutionResponse(
        Map<String, Object> outputs,
        List<String> firedRules,
        long executionTimeMs,
        String ruleSetKey,
        String tenantId
) {}
