package com.platform.data.entity.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEntityDefinitionRequest(
        @NotBlank String displayName,
        @NotBlank String schema
) {}
