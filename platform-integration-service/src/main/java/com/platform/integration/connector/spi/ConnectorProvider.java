package com.platform.integration.connector.spi;

import com.platform.integration.connector.ConnectorDefinition;
import com.platform.integration.connector.ConnectorType;

import java.util.List;
import java.util.Map;

/**
 * SPI contract for integration connectors. Implement this interface and
 * register the class as a Spring bean to make a connector available at runtime.
 * No changes to {@link com.platform.integration.engine.CamelRouteEngine} or
 * any other core class are required when adding a new connector.
 *
 * <p>Route execution model:
 * <pre>
 *   from(sourceUri)
 *     [.to(uri) for each uri in intermediateUris]   // e.g. HTTP polling fetch
 *     .to(targetUri)
 * </pre>
 */
public interface ConnectorProvider {

    /** Identifies which connector type this provider handles. */
    ConnectorType connectorType();

    /** Metadata exposed through the connector catalog REST API. */
    ConnectorDefinition definition();

    /**
     * Returns the Camel {@code from()} URI when this connector is the data source.
     * May be a scheduler, SFTP poller, SQL consumer, IMAP listener, etc.
     */
    String sourceUri(Map<String, Object> config);

    /**
     * URIs inserted between source and target, in order.
     * Used by polling connectors (HTTP, Slack) where the source is a scheduler
     * and the actual data-fetch call is an intermediate {@code .to()} step.
     */
    default List<String> intermediateUris(Map<String, Object> config) {
        return List.of();
    }

    /**
     * Returns the Camel {@code to()} URI when this connector is the data target.
     */
    String targetUri(Map<String, Object> config);
}
