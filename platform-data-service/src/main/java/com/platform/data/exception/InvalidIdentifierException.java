package com.platform.data.exception;

/** An entity type, field name, or derived table/column name failed identifier validation. */
public class InvalidIdentifierException extends RuntimeException {
    public InvalidIdentifierException(String message) {
        super(message);
    }
}
