package com.platform.studio.artifact.bridge;

import java.util.List;

/** Mirrors platform-studio-frontend's FormDesigner FieldDefinition shape. */
public record StudioFormField(
        String id,
        String type,
        String label,
        String name,
        String placeholder,
        Boolean required,
        List<String> options,
        Integer rows,
        List<StudioFormField> children
) {}
