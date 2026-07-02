package com.platform.page.page.dto;

import com.platform.page.page.domain.PageStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PageDefinitionDto(
        UUID id,
        String tenantId,
        String pageKey,
        String name,
        String description,
        String schema,
        PageStatus status,
        int version,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
