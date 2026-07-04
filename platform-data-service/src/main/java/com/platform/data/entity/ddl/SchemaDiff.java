package com.platform.data.entity.ddl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Classifies an old-vs-new field list into what changed. Additive-only edits are supported;
 * anything lossy against existing rows (a dropped property, a type change, or tightening
 * "required" on a property that already existed as optional) is rejected rather than attempted —
 * this codebase has no lossy-migration/versioning tooling to fall back on.
 */
@Component
public class SchemaDiff {

    public record SchemaDiffResult(
            List<FieldSchema> added,
            List<String> removed,
            List<String> typeChanged,
            List<String> newlyRequiredExisting) {

        public boolean isAdditive() {
            return removed.isEmpty() && typeChanged.isEmpty() && newlyRequiredExisting.isEmpty();
        }
    }

    public SchemaDiffResult diff(List<FieldSchema> oldFields, List<FieldSchema> newFields) {
        Map<String, FieldSchema> oldByName = toMap(oldFields);
        Map<String, FieldSchema> newByName = toMap(newFields);

        List<FieldSchema> added = new ArrayList<>();
        List<String> typeChanged = new ArrayList<>();
        List<String> newlyRequiredExisting = new ArrayList<>();

        for (FieldSchema newField : newFields) {
            FieldSchema oldField = oldByName.get(newField.name());
            if (oldField == null) {
                added.add(newField);
            } else if (!oldField.type().equals(newField.type())) {
                typeChanged.add(newField.name());
            } else if (!oldField.required() && newField.required()) {
                newlyRequiredExisting.add(newField.name());
            }
        }

        List<String> removed = new ArrayList<>();
        for (FieldSchema oldField : oldFields) {
            if (!newByName.containsKey(oldField.name())) {
                removed.add(oldField.name());
            }
        }

        return new SchemaDiffResult(added, removed, typeChanged, newlyRequiredExisting);
    }

    private Map<String, FieldSchema> toMap(List<FieldSchema> fields) {
        return fields.stream().collect(java.util.stream.Collectors.toMap(FieldSchema::name, Function.identity()));
    }
}
