package com.platform.data.exception;

import java.util.List;

/**
 * A PUT to /api/v1/entities/definitions/{entityType} attempted a non-additive schema change
 * (dropped a property, changed a property's type, or newly-required an already-existing
 * property) — all lossy/dangerous against existing rows, and rejected rather than attempted.
 */
public class NonAdditiveSchemaChangeException extends RuntimeException {

    private final List<String> removedProperties;
    private final List<String> typeChangedProperties;
    private final List<String> newlyRequiredExistingProperties;

    public NonAdditiveSchemaChangeException(List<String> removedProperties,
                                             List<String> typeChangedProperties,
                                             List<String> newlyRequiredExistingProperties) {
        super("Schema change is not additive-only");
        this.removedProperties = removedProperties;
        this.typeChangedProperties = typeChangedProperties;
        this.newlyRequiredExistingProperties = newlyRequiredExistingProperties;
    }

    public List<String> removedProperties() {
        return removedProperties;
    }

    public List<String> typeChangedProperties() {
        return typeChangedProperties;
    }

    public List<String> newlyRequiredExistingProperties() {
        return newlyRequiredExistingProperties;
    }
}
