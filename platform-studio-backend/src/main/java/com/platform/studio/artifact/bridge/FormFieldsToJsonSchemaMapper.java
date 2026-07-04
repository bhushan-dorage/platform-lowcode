package com.platform.studio.artifact.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates Studio's Form Designer save shape ({@code {formKey, fields: [...]}}) into the
 * JSON-Schema + uiSchema pair platform-form-service's create/publish endpoints expect. Pure
 * function over the parsed field list — no I/O.
 */
@Component
@RequiredArgsConstructor
public class FormFieldsToJsonSchemaMapper {

    private final ObjectMapper objectMapper;

    public FormPublishPayload map(String rawContent) {
        try {
            StudioFormSchema schema = objectMapper.readValue(rawContent, StudioFormSchema.class);
            List<StudioFormField> fields = schema.fields() != null ? schema.fields() : List.of();

            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (StudioFormField field : fields) {
                properties.put(field.name(), toSchemaProperty(field));
                if (Boolean.TRUE.equals(field.required())) {
                    required.add(field.name());
                }
            }

            Map<String, Object> jsonSchema = new LinkedHashMap<>();
            jsonSchema.put("type", "object");
            jsonSchema.put("title", schema.formKey());
            jsonSchema.put("properties", properties);
            if (!required.isEmpty()) {
                jsonSchema.put("required", required);
            }

            List<Map<String, Object>> uiSchema = fields.stream().map(this::toUiEntry).toList();

            return new FormPublishPayload(
                    objectMapper.writeValueAsString(jsonSchema),
                    objectMapper.writeValueAsString(uiSchema));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Form artifact content is not a valid Studio form schema", ex);
        }
    }

    private Map<String, Object> toSchemaProperty(StudioFormField field) {
        Map<String, Object> property = new LinkedHashMap<>();
        switch (field.type()) {
            case "number" -> property.put("type", "number");
            case "checkbox" -> property.put("type", "boolean");
            case "date" -> {
                property.put("type", "string");
                property.put("format", "date");
            }
            case "email" -> {
                property.put("type", "string");
                property.put("format", "email");
            }
            case "select" -> {
                property.put("type", "string");
                property.put("enum", field.options() != null ? field.options() : List.of());
            }
            case "section" -> {
                property.put("type", "object");
                property.put("title", field.label());
                Map<String, Object> childProperties = new LinkedHashMap<>();
                List<String> childRequired = new ArrayList<>();
                List<StudioFormField> children = field.children() != null ? field.children() : List.of();
                for (StudioFormField child : children) {
                    childProperties.put(child.name(), toSchemaProperty(child));
                    if (Boolean.TRUE.equals(child.required())) {
                        childRequired.add(child.name());
                    }
                }
                property.put("properties", childProperties);
                if (!childRequired.isEmpty()) {
                    property.put("required", childRequired);
                }
            }
            default -> property.put("type", "string"); // text, textarea
        }
        if (field.label() != null && !"section".equals(field.type())) {
            property.put("title", field.label());
        }
        return property;
    }

    private Map<String, Object> toUiEntry(StudioFormField field) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("field", field.name());
        entry.put("widgetType", field.type());
        return entry;
    }
}
