package com.platform.studio.artifact.dto;

import java.util.Map;

public record CreateBundleRequest(
        String version,
        /** Map of artifactId (UUID string) → semantic version to pin */
        Map<String, String> artifactVersions
) {}
