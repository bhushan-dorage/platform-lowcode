package com.platform.data.entity.ddl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaDiffTest {

    private final SchemaDiff schemaDiff = new SchemaDiff();

    @Test
    void diff_noChanges_isAdditive() {
        List<FieldSchema> fields = List.of(new FieldSchema("amount", "number", true));
        var result = schemaDiff.diff(fields, fields);

        assertThat(result.isAdditive()).isTrue();
        assertThat(result.added()).isEmpty();
    }

    @Test
    void diff_newProperty_isAdditiveWithAddedField() {
        List<FieldSchema> oldFields = List.of(new FieldSchema("amount", "number", true));
        List<FieldSchema> newFields = List.of(
                new FieldSchema("amount", "number", true),
                new FieldSchema("currency", "string", false));

        var result = schemaDiff.diff(oldFields, newFields);

        assertThat(result.isAdditive()).isTrue();
        assertThat(result.added()).extracting(FieldSchema::name).containsExactly("currency");
    }

    @Test
    void diff_removedProperty_isNotAdditive() {
        List<FieldSchema> oldFields = List.of(
                new FieldSchema("amount", "number", true),
                new FieldSchema("currency", "string", false));
        List<FieldSchema> newFields = List.of(new FieldSchema("amount", "number", true));

        var result = schemaDiff.diff(oldFields, newFields);

        assertThat(result.isAdditive()).isFalse();
        assertThat(result.removed()).containsExactly("currency");
    }

    @Test
    void diff_typeChanged_isNotAdditive() {
        List<FieldSchema> oldFields = List.of(new FieldSchema("amount", "number", true));
        List<FieldSchema> newFields = List.of(new FieldSchema("amount", "string", true));

        var result = schemaDiff.diff(oldFields, newFields);

        assertThat(result.isAdditive()).isFalse();
        assertThat(result.typeChanged()).containsExactly("amount");
    }

    @Test
    void diff_newlyRequiredExistingProperty_isNotAdditive() {
        List<FieldSchema> oldFields = List.of(new FieldSchema("amount", "number", false));
        List<FieldSchema> newFields = List.of(new FieldSchema("amount", "number", true));

        var result = schemaDiff.diff(oldFields, newFields);

        assertThat(result.isAdditive()).isFalse();
        assertThat(result.newlyRequiredExisting()).containsExactly("amount");
    }

    @Test
    void diff_existingPropertyBecomingLessRequired_isAdditive() {
        List<FieldSchema> oldFields = List.of(new FieldSchema("amount", "number", true));
        List<FieldSchema> newFields = List.of(new FieldSchema("amount", "number", false));

        var result = schemaDiff.diff(oldFields, newFields);

        assertThat(result.isAdditive()).isTrue();
    }
}
