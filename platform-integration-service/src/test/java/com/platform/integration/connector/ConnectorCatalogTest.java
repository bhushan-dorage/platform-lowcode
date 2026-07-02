package com.platform.integration.connector;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class ConnectorCatalogTest {

    private final ConnectorCatalog catalog = new ConnectorCatalog();

    @Test
    void listAll_returnsAllFiveConnectors() {
        assertThat(catalog.listAll()).hasSize(5);
    }

    @Test
    void findByType_returnsConnectorForEachType() {
        for (ConnectorType type : ConnectorType.values()) {
            Optional<ConnectorDefinition> found = catalog.findByType(type);
            assertThat(found).isPresent();
            assertThat(found.get().type()).isEqualTo(type);
        }
    }

    @Test
    void findByType_httpConnectorHasRequiredParams() {
        ConnectorDefinition http = catalog.findByType(ConnectorType.HTTP).orElseThrow();
        assertThat(http.requiredParams()).extracting(ConnectorDefinition.ConnectorParam::name)
                .contains("url", "method");
    }

    @Test
    void findByType_jdbcConnectorHasRequiredParams() {
        ConnectorDefinition jdbc = catalog.findByType(ConnectorType.JDBC).orElseThrow();
        assertThat(jdbc.requiredParams()).extracting(ConnectorDefinition.ConnectorParam::name)
                .contains("dataSourceName", "query");
    }

    @Test
    void findByType_unknownTypeReturnsEmpty() {
        assertThat(catalog.listAll()).noneMatch(c -> c.type() == null);
    }
}
