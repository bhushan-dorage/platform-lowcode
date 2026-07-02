package com.platform.integration.connector;

import com.platform.integration.connector.spi.ConnectorProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Exposes connector metadata to the REST API. Delegates entirely to
 * {@link ConnectorProviderRegistry} so the catalog reflects whatever
 * providers are registered in the Spring context — no hardcoded list.
 */
@Component
@RequiredArgsConstructor
public class ConnectorCatalog {

    private final ConnectorProviderRegistry registry;

    public List<ConnectorDefinition> listAll() {
        return registry.all().stream()
                .map(p -> p.definition())
                .sorted(Comparator.comparing(d -> d.type().name()))
                .toList();
    }

    public Optional<ConnectorDefinition> findByType(ConnectorType type) {
        try {
            return Optional.of(registry.get(type).definition());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }
}
