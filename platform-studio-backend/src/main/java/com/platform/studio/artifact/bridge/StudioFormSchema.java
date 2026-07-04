package com.platform.studio.artifact.bridge;

import java.util.List;

/** Mirrors platform-studio-frontend's FormDesigner save payload: { formKey, fields }. */
public record StudioFormSchema(String formKey, List<StudioFormField> fields) {}
