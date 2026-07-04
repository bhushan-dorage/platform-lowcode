package com.platform.data.entity.ddl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.data.exception.UnsupportedFieldTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parses an EntityDefinition's JSON-Schema "schema" string into a flat list of FieldSchema. */
@Component
@RequiredArgsConstructor
public class JsonSchemaParser {

    private final ObjectMapper objectMapper;

    public List<FieldSchema> parse(String schemaJson) {
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            JsonNode properties = root.path("properties");
            Set<String> required = new HashSet<>();
            for (JsonNode r : root.path("required")) {
                required.add(r.asText());
            }

            List<FieldSchema> fields = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> it = properties.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String name = entry.getKey();
                String type = entry.getValue().path("type").asText();
                if (!FieldSchema.isSupportedType(type)) {
                    throw new UnsupportedFieldTypeException(
                            "Unsupported field type '" + type + "' for property '" + name + "'");
                }
                fields.add(new FieldSchema(name, type, required.contains(name)));
            }
            return fields;
        } catch (UnsupportedFieldTypeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("schema is not valid JSON Schema: " + ex.getMessage(), ex);
        }
    }
}
