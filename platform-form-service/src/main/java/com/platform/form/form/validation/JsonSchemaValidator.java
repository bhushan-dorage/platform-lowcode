package com.platform.form.form.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.platform.form.form.dto.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonSchemaValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    public ValidationResult validate(String jsonSchema, Map<String, Object> data) {
        try {
            JsonNode schemaNode = objectMapper.readTree(jsonSchema);
            JsonNode dataNode = objectMapper.valueToTree(data);
            Set<ValidationMessage> messages = schemaFactory.getSchema(schemaNode).validate(dataNode);
            if (messages.isEmpty()) return ValidationResult.ok();
            List<String> errors = messages.stream().map(ValidationMessage::getMessage).toList();
            return ValidationResult.failed(errors);
        } catch (Exception e) {
            log.error("JSON Schema validation error", e);
            return ValidationResult.failed(List.of("Schema validation failed: " + e.getMessage()));
        }
    }

    /** Validates that the schema string is itself valid JSON Schema. */
    public void assertValidSchema(String jsonSchema) {
        try {
            JsonNode schemaNode = objectMapper.readTree(jsonSchema);
            schemaFactory.getSchema(schemaNode); // throws if invalid
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON Schema: " + e.getMessage());
        }
    }
}
