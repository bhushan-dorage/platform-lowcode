package com.platform.data.exception;

/** A JSON-Schema property declared a "type" outside {string,number,boolean,date,object,array}. */
public class UnsupportedFieldTypeException extends RuntimeException {
    public UnsupportedFieldTypeException(String message) {
        super(message);
    }
}
