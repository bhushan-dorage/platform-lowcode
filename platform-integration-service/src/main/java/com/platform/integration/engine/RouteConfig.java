package com.platform.integration.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Parsed form of the {@code routeDefinition} JSON column stored in
 * {@link com.platform.integration.route.RouteDefinitionEntity}.
 *
 * <p>Expected JSON shape:
 * <pre>
 * {
 *   "source": { "url": "https://api.example.com/orders", "method": "GET" },
 *   "target": { "to": "admin@example.com", "subject": "New order" }
 * }
 * </pre>
 * {@code source} keys map to the required/optional params of the source
 * {@link com.platform.integration.connector.spi.ConnectorProvider};
 * {@code target} keys map to the target provider.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteConfig(
        Map<String, Object> source,
        Map<String, Object> target) {

    public static final RouteConfig EMPTY = new RouteConfig(Map.of(), Map.of());

    public static RouteConfig parse(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return EMPTY;
        }
        try {
            RouteConfig parsed = mapper.readValue(json, RouteConfig.class);
            return new RouteConfig(
                    parsed.source() != null ? parsed.source() : Map.of(),
                    parsed.target() != null ? parsed.target() : Map.of());
        } catch (Exception e) {
            return EMPTY;
        }
    }
}
