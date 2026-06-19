package com.platform.common.web;

import java.time.Instant;

/**
 * Canonical response wrapper for all successful API responses.
 *
 * Every response carries tracing identifiers so observability tools can correlate
 * logs, metrics, and traces without relying on HTTP headers alone.
 */
public record StandardResponseEnvelope<T>(T data, Meta meta) {

    public record Meta(String requestId, String traceId, Instant timestamp) {}

    public static <T> StandardResponseEnvelope<T> of(T data, String requestId, String traceId) {
        return new StandardResponseEnvelope<>(data, new Meta(requestId, traceId, Instant.now()));
    }
}
