package com.platform.integration.connector.impl;

import com.platform.integration.connector.ConnectorDefinition;
import com.platform.integration.connector.ConnectorDefinition.ConnectorParam;
import com.platform.integration.connector.ConnectorType;
import com.platform.integration.connector.spi.ConnectorProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * HTTP/REST connector.
 *
 * <p>As a <b>source</b>: polls a remote URL on a configurable schedule using
 * a Camel {@code scheduler} component, then fetches data via HTTP GET in an
 * intermediate step.
 *
 * <p>As a <b>target</b>: POSTs the route body to the configured URL.
 */
@Component
public class HttpConnectorProvider implements ConnectorProvider {

    private static final ConnectorDefinition DEFINITION = new ConnectorDefinition(
            ConnectorType.HTTP,
            "HTTP/REST",
            "Call any HTTP/REST endpoint or poll a remote API on a schedule",
            List.of(
                    new ConnectorParam("url", "string", "Target URL (including protocol)"),
                    new ConnectorParam("method", "enum[GET,POST,PUT,DELETE]", "HTTP method")),
            List.of(
                    new ConnectorParam("pollPeriodSeconds", "integer", "Poll interval in seconds when used as source (default 30)"),
                    new ConnectorParam("headers", "map", "Additional HTTP headers"),
                    new ConnectorParam("timeout", "integer", "Connection timeout in milliseconds (default 10000)")));

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.HTTP;
    }

    @Override
    public ConnectorDefinition definition() {
        return DEFINITION;
    }

    /**
     * Source is a scheduler that fires on the configured period; the actual
     * HTTP fetch is done in {@link #intermediateUris}.
     */
    @Override
    public String sourceUri(Map<String, Object> config) {
        long periodMs = Long.parseLong(config.getOrDefault("pollPeriodSeconds", "30").toString()) * 1000L;
        return "scheduler://http-poll?delay=0&period=" + periodMs;
    }

    /**
     * The HTTP fetch that runs after the scheduler fires.
     */
    @Override
    public List<String> intermediateUris(Map<String, Object> config) {
        String url = requireString(config, "url");
        String method = config.getOrDefault("method", "GET").toString();
        int timeout = Integer.parseInt(config.getOrDefault("timeout", "10000").toString());
        return List.of(url + (url.contains("?") ? "&" : "?")
                + "httpMethod=" + method
                + "&connectTimeout=" + timeout
                + "&throwExceptionOnFailure=true");
    }

    @Override
    public String targetUri(Map<String, Object> config) {
        String url = requireString(config, "url");
        String method = config.getOrDefault("method", "POST").toString();
        int timeout = Integer.parseInt(config.getOrDefault("timeout", "10000").toString());
        return url + (url.contains("?") ? "&" : "?")
                + "httpMethod=" + method
                + "&connectTimeout=" + timeout
                + "&throwExceptionOnFailure=true";
    }

    private static String requireString(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("HTTP connector requires '" + key + "'");
        }
        return val.toString();
    }
}
