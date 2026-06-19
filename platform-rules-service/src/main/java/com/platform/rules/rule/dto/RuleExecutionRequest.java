package com.platform.rules.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RuleExecutionRequest(
        @NotBlank String ruleSetKey,
        @NotNull Map<String, Object> inputs,
        String containerId   // KIE container override; defaults to ruleSetKey
) {}
