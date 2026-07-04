package com.platform.data.entity.ddl;

import com.platform.data.exception.InvalidIdentifierException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates and quotes identifiers before they're ever interpolated into dynamic DDL. JDBC can
 * parameterize values, never identifiers — this allowlist is the only thing standing between
 * user-supplied entity/field names and a CREATE TABLE/ALTER TABLE string.
 *
 * The allowlist is deliberately ASCII-only ([a-z][a-z0-9_]{0,62}), which guarantees 1 byte per
 * character, so a 63-character match is always <=63 bytes — Postgres's NAMEDATALEN limit is
 * respected by construction rather than by a separate byte-length check.
 */
public final class SqlIdentifiers {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    /** Platform-managed columns every generated table already has — user field names can't collide with these. */
    private static final Set<String> RESERVED_COLUMN_NAMES = Set.of(
            "id", "tenant_id", "archived_at", "created_by", "created_at", "updated_at");

    private SqlIdentifiers() {}

    public static void validate(String identifier, String kind) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new InvalidIdentifierException(
                    kind + " must match ^[a-z][a-z0-9_]{0,62}$: " + identifier);
        }
    }

    public static void validateFieldName(String fieldName) {
        validate(fieldName, "Field name");
        if (RESERVED_COLUMN_NAMES.contains(fieldName)) {
            throw new InvalidIdentifierException(
                    "Field name is reserved for platform use: " + fieldName);
        }
    }

    /** Wraps an already-validated identifier in double quotes (defense-in-depth + reserved-word safety). */
    public static String quote(String identifier) {
        return "\"" + identifier + "\"";
    }
}
