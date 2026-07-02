package com.platform.integration.connector.spi;

import com.platform.integration.connector.ConnectorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Discovers all {@link ConnectorProvider} beans registered in the Spring context
 * and indexes them by {@link ConnectorType}. Services call {@link #get} to look
 * up the provider for a given connector type at route-start time.
 */
@Component
public class ConnectorProviderRegistry {

    private final Map<ConnectorType, ConnectorProvider> providers;

    public ConnectorProviderRegistry(List<ConnectorProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(ConnectorProvider::connectorType, Function.identity()));
    }

    public ConnectorProvider get(ConnectorType type) {
        ConnectorProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalStateException("No ConnectorProvider registered for type: " + type);
        }
        return provider;
    }

    public List<ConnectorProvider> all() {
        return List.copyOf(providers.values());
    }
}
