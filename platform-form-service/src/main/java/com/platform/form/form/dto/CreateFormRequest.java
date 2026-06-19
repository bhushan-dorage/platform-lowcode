package com.platform.form.form.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFormRequest(
        @NotBlank String formKey,
        @NotBlank String name,
        String description,
        @NotBlank String jsonSchema,
        String uiSchema
) {}
