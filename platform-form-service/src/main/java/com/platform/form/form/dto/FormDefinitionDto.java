package com.platform.form.form.dto;

import com.platform.form.form.domain.FormStatus;
import java.time.Instant;
import java.util.UUID;

public record FormDefinitionDto(
        UUID id, String tenantId, String formKey, String name, String description,
        int currentVersion, FormStatus status, String createdBy, Instant createdAt, Instant updatedAt
) {}
