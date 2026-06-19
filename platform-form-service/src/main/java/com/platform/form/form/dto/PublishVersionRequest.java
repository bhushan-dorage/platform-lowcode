package com.platform.form.form.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishVersionRequest(@NotBlank String jsonSchema, String uiSchema) {}
