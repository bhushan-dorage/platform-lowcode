package com.platform.workflow.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskAssignRequest(@NotBlank String assignee) {}
