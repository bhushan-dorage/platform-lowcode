package com.platform.workflow.exception;

import com.platform.common.web.ErrorResponseEnvelope;
import com.platform.common.tenant.TenantNotFoundException;
import com.platform.workflow.task.TaskAlreadyClaimedException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskAlreadyClaimedException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleTaskAlreadyClaimed(TaskAlreadyClaimedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseEnvelope.of("TASK_ALREADY_CLAIMED", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseEnvelope.of("RESOURCE_NOT_FOUND", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleInvalidTenant(TenantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseEnvelope.of("INVALID_TENANT", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"));
        var envelope = new ErrorResponseEnvelope(
                new ErrorResponseEnvelope.ErrorDetail("VALIDATION_ERROR", "Request validation failed", traceId(), fieldErrors));
        return ResponseEntity.badRequest().body(envelope);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseEnvelope> handleGeneric(Exception ex) {
        log.error("Unhandled exception [traceId={}]", traceId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseEnvelope.of("INTERNAL_ERROR", "An unexpected error occurred", traceId()));
    }

    private String traceId() {
        return MDC.get("traceId");
    }
}
