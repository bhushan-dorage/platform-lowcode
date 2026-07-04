package com.platform.data.entity.ddl;

import com.platform.data.exception.InvalidIdentifierException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlIdentifiersTest {

    @Test
    void validate_acceptsLowercaseAlphanumericUnderscore() {
        assertThatCode(() -> SqlIdentifiers.validate("invoice_line_item", "Table name")).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsUppercase() {
        assertThatThrownBy(() -> SqlIdentifiers.validate("Invoice", "Table name"))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void validate_rejectsLeadingDigit() {
        assertThatThrownBy(() -> SqlIdentifiers.validate("1invoice", "Table name"))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void validate_rejectsSqlInjectionAttempt() {
        assertThatThrownBy(() -> SqlIdentifiers.validate("invoice\"; DROP TABLE users; --", "Table name"))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void validate_rejectsNull() {
        assertThatThrownBy(() -> SqlIdentifiers.validate(null, "Table name"))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void validate_accepts63CharIdentifier() {
        String sixtyThreeChars = "a".repeat(63);
        assertThatCode(() -> SqlIdentifiers.validate(sixtyThreeChars, "Table name")).doesNotThrowAnyException();
    }

    @Test
    void validate_rejects64CharIdentifier() {
        String sixtyFourChars = "a".repeat(64);
        assertThatThrownBy(() -> SqlIdentifiers.validate(sixtyFourChars, "Table name"))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void validateFieldName_rejectsReservedColumnNames() {
        assertThatThrownBy(() -> SqlIdentifiers.validateFieldName("tenant_id"))
                .isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> SqlIdentifiers.validateFieldName("archived_at"))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void quote_wrapsInDoubleQuotes() {
        assertThat(SqlIdentifiers.quote("invoice")).isEqualTo("\"invoice\"");
    }
}
