package com.platform.studio.artifact.dto;

import com.platform.studio.artifact.domain.ArtifactType;

public record SaveArtifactRequest(
        ArtifactType type,
        String name,
        String displayName,
        String description,
        String content
) {}
