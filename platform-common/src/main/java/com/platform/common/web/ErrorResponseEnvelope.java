package com.platform.common.web;

/**
 * Canonical error wrapper returned on all non-2xx responses.
 *
 * The traceId field links the response to the distributed trace so support teams
 * can find the full call stack in the tracing backend without querying logs first.
 */
public record ErrorResponseEnvelope(ErrorDetail error) {

    public record ErrorDetail(String code, String message, String traceId, Object details) {}

    public static ErrorResponseEnvelope of(String code, String message, String traceId) {
        return new ErrorResponseEnvelope(new ErrorDetail(code, message, traceId, null));
    }
}
