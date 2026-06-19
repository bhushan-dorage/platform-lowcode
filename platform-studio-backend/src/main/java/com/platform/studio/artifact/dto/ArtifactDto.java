package com.platform.studio.artifact.dto;

import com.platform.studio.artifact.domain.Artifact;
import com.platform.studio.artifact.domain.ArtifactStatus;
import com.platform.studio.artifact.domain.ArtifactType;

import java.time.Instant;
import java.util.UUID;

public record ArtifactDto(
        UUID id,
        String tenantId,
        ArtifactType type,
        String name,
        String displayName,
        String description,
        String currentVersion,
        String headCommitSha,
        ArtifactStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
    public static ArtifactDto from(Artifact a) {
        return new ArtifactDto(a.getId(), a.getTenantId(), a.getType(), a.getName(),
                a.getDisplayName(), a.getDescription(), a.getCurrentVersion(),
                a.getHeadCommitSha(), a.getStatus(), a.getCreatedBy(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getPublishedAt());
    }
}
