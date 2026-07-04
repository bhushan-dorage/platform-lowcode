package com.platform.data.entity.ddl;

/** One property parsed out of an EntityDefinition's JSON-Schema "schema" string. */
public record FieldSchema(String name, String type, boolean required) {

    /** Field types the Studio Data Modeler UI can produce; anything else is rejected. */
    public static boolean isSupportedType(String type) {
        return switch (type) {
            case "string", "number", "boolean", "date", "object", "array" -> true;
            default -> false;
        };
    }
}
