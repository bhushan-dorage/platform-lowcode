package com.platform.page.exception;

import com.platform.common.web.ErrorResponseEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseEnvelope.of("RESOURCE_NOT_FOUND", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleBindValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(),
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid"));
        var envelope = new ErrorResponseEnvelope(new ErrorResponseEnvelope.ErrorDetail(
                "VALIDATION_ERROR", "Request validation failed", MDC.get("traceId"), fields));
        return ResponseEntity.badRequest().body(envelope);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponseEnvelope.of("BAD_REQUEST", ex.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseEnvelope> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseEnvelope.of("INTERNAL_ERROR", "An unexpected error occurred", MDC.get("traceId")));
    }
}
