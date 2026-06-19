package com.platform.studio.artifact.dto;

import com.platform.studio.artifact.domain.ArtifactType;

public record ArtifactContentDto(ArtifactDto metadata, String content) {}
