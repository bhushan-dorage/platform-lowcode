package com.platform.studio.artifact.bridge;

/** JSON-Schema-shaped form definition ready to publish to platform-form-service. */
public record FormPublishPayload(String jsonSchema, String uiSchema) {}
