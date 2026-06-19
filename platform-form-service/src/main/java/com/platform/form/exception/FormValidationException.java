package com.platform.form.exception;

import java.util.List;

public class FormValidationException extends RuntimeException {
    private final List<String> errors;
    public FormValidationException(List<String> errors) {
        super("Form validation failed");
        this.errors = errors;
    }
    public List<String> getErrors() { return errors; }
}
