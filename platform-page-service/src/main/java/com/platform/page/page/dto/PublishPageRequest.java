package com.platform.page.page.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishPageRequest(
        @NotBlank String schema
) {}
