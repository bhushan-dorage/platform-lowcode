package com.platform.data.entity.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEntityDefinitionRequest(
        @NotBlank String entityType,
        @NotBlank String displayName,
        @NotBlank String schema
) {}
