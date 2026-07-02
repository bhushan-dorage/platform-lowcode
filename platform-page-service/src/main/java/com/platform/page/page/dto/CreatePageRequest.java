package com.platform.page.page.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePageRequest(
        @NotBlank String pageKey,
        @NotBlank String name,
        String description,
        @NotBlank String schema
) {}
