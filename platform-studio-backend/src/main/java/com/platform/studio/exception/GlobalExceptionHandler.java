package com.platform.studio.exception;

import com.platform.common.web.ErrorResponseEnvelope;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseEnvelope.of("CONFLICT", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(RuntimeServiceBridgeException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleRuntimeServiceBridge(RuntimeServiceBridgeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponseEnvelope.of("RUNTIME_SERVICE_UNAVAILABLE", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseEnvelope> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseEnvelope.of("INTERNAL_ERROR", "An unexpected error occurred", MDC.get("traceId")));
    }
}
