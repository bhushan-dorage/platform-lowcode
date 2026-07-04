package com.platform.rules.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RuleExecutionRequest(
        @NotBlank String ruleSetKey,
        @NotNull Map<String, Object> inputs,
        @Deprecated String containerId   // KIE-only concept; ignored by the embedded Flowable DMN engine
) {}
