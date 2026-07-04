package com.platform.data.exception;

import com.platform.common.web.ErrorResponseEnvelope;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseEnvelope.of("NOT_FOUND", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseEnvelope.of("BAD_REQUEST", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(InvalidIdentifierException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleInvalidIdentifier(InvalidIdentifierException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseEnvelope.of("INVALID_IDENTIFIER", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(UnsupportedFieldTypeException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleUnsupportedFieldType(UnsupportedFieldTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseEnvelope.of("UNSUPPORTED_FIELD_TYPE", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(NonAdditiveSchemaChangeException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleNonAdditiveSchemaChange(NonAdditiveSchemaChangeException ex) {
        Map<String, Object> details = Map.of(
                "removedProperties", ex.removedProperties(),
                "typeChangedProperties", ex.typeChangedProperties(),
                "newlyRequiredExistingProperties", ex.newlyRequiredExistingProperties()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseEnvelope(new ErrorResponseEnvelope.ErrorDetail(
                        "NON_ADDITIVE_SCHEMA_CHANGE", ex.getMessage(), MDC.get("traceId"), details)));
    }
}
