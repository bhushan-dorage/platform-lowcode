package com.platform.workflow.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskEscalateRequest(@NotBlank String escalateTo, String reason) {}
