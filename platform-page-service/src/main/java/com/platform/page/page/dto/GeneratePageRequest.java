package com.platform.page.page.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneratePageRequest(
        @NotBlank @Size(max = 2000) String prompt
) {}
