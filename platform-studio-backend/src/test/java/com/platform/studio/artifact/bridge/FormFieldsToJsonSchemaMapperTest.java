package com.platform.studio.artifact.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormFieldsToJsonSchemaMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FormFieldsToJsonSchemaMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new FormFieldsToJsonSchemaMapper(objectMapper);
    }

    @Test
    void mapsBasicFieldTypesToJsonSchema() throws Exception {
        String content = """
                {
                  "formKey": "intake-form",
                  "fields": [
                    {"id":"1","type":"text","label":"Name","name":"name","required":true},
                    {"id":"2","type":"number","label":"Age","name":"age","required":false},
                    {"id":"3","type":"checkbox","label":"Active","name":"active"},
                    {"id":"4","type":"select","label":"Region","name":"region","options":["APAC","EMEA"]},
                    {"id":"5","type":"email","label":"Email","name":"email"}
                  ]
                }
                """;

        FormPublishPayload payload = mapper.map(content);

        JsonNode schema = objectMapper.readTree(payload.jsonSchema());
        assertThat(schema.get("type").asText()).isEqualTo("object");
        assertThat(schema.get("title").asText()).isEqualTo("intake-form");
        assertThat(schema.get("properties").get("name").get("type").asText()).isEqualTo("string");
        assertThat(schema.get("properties").get("age").get("type").asText()).isEqualTo("number");
        assertThat(schema.get("properties").get("active").get("type").asText()).isEqualTo("boolean");
        assertThat(schema.get("properties").get("region").get("enum").toString()).contains("APAC", "EMEA");
        assertThat(schema.get("properties").get("email").get("format").asText()).isEqualTo("email");

        JsonNode required = schema.get("required");
        assertThat(required).hasSize(1);
        assertThat(required.get(0).asText()).isEqualTo("name");

        JsonNode uiSchema = objectMapper.readTree(payload.uiSchema());
        assertThat(uiSchema).hasSize(5);
        assertThat(uiSchema.get(0).get("field").asText()).isEqualTo("name");
        assertThat(uiSchema.get(0).get("widgetType").asText()).isEqualTo("text");
    }

    @Test
    void mapsSectionAsNestedObject() throws Exception {
        String content = """
                {
                  "formKey": "nested-form",
                  "fields": [
                    {"id":"1","type":"section","label":"Address","name":"address","children":[
                      {"id":"2","type":"text","label":"Street","name":"street","required":true}
                    ]}
                  ]
                }
                """;

        FormPublishPayload payload = mapper.map(content);
        JsonNode schema = objectMapper.readTree(payload.jsonSchema());
        JsonNode addressProp = schema.get("properties").get("address");
        assertThat(addressProp.get("type").asText()).isEqualTo("object");
        assertThat(addressProp.get("properties").get("street").get("type").asText()).isEqualTo("string");
        assertThat(addressProp.get("required").get(0).asText()).isEqualTo("street");
    }

    @Test
    void invalidContent_throwsIllegalArgument() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> mapper.map("not json"));
    }
}
