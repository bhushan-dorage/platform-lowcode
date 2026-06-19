package com.platform.entitlements.exception;

import com.platform.common.web.ErrorResponseEnvelope;
import com.platform.entitlements.enforcement.AccessDeniedException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseEnvelope.of("ACCESS_DENIED", ex.getMessage(), MDC.get("traceId"), null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseEnvelope.of("NOT_FOUND", ex.getMessage(), MDC.get("traceId"), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseEnvelope.of("BAD_REQUEST", ex.getMessage(), MDC.get("traceId"), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseEnvelope> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseEnvelope.of("INTERNAL_ERROR", "An unexpected error occurred", MDC.get("traceId"), null));
    }
}
