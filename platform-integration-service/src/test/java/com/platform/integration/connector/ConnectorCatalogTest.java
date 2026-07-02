package com.platform.integration.connector;

import com.platform.integration.connector.impl.EmailConnectorProvider;
import com.platform.integration.connector.impl.HttpConnectorProvider;
import com.platform.integration.connector.impl.JdbcConnectorProvider;
import com.platform.integration.connector.impl.SlackConnectorProvider;
import com.platform.integration.connector.impl.SftpConnectorProvider;
import com.platform.integration.connector.spi.ConnectorProvider;
import com.platform.integration.connector.spi.ConnectorProviderRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectorCatalogTest {

    private final List<ConnectorProvider> providers = List.of(
            new HttpConnectorProvider(),
            new JdbcConnectorProvider(),
            new SftpConnectorProvider(),
            new EmailConnectorProvider(),
            new SlackConnectorProvider());

    private final ConnectorCatalog catalog =
            new ConnectorCatalog(new ConnectorProviderRegistry(providers));

    @Test
    void listAll_returnsAllFiveConnectors() {
        assertThat(catalog.listAll()).hasSize(5);
    }

    @Test
    void findByType_returnsConnectorForEachType() {
        for (ConnectorType type : ConnectorType.values()) {
            Optional<ConnectorDefinition> found = catalog.findByType(type);
            assertThat(found).as("provider for %s", type).isPresent();
            assertThat(found.get().type()).isEqualTo(type);
        }
    }

    @Test
    void findByType_httpConnectorHasRequiredParams() {
        ConnectorDefinition http = catalog.findByType(ConnectorType.HTTP).orElseThrow();
        assertThat(http.requiredParams())
                .extracting(ConnectorDefinition.ConnectorParam::name)
                .contains("url", "method");
    }

    @Test
    void findByType_jdbcConnectorHasRequiredParams() {
        ConnectorDefinition jdbc = catalog.findByType(ConnectorType.JDBC).orElseThrow();
        assertThat(jdbc.requiredParams())
                .extracting(ConnectorDefinition.ConnectorParam::name)
                .contains("dataSourceName", "query");
    }

    @Test
    void allProviders_haveNonBlankDisplayNameAndDescription() {
        catalog.listAll().forEach(def -> {
            assertThat(def.displayName()).as("displayName for %s", def.type()).isNotBlank();
            assertThat(def.description()).as("description for %s", def.type()).isNotBlank();
        });
    }

    @Test
    void httpProvider_buildSourceUri_containsScheduler() {
        ConnectorProvider http = providers.stream()
                .filter(p -> p.connectorType() == ConnectorType.HTTP).findFirst().orElseThrow();
        String uri = http.sourceUri(java.util.Map.of("url", "https://api.example.com", "method", "GET"));
        assertThat(uri).startsWith("scheduler://");
    }

    @Test
    void httpProvider_buildTargetUri_containsUrl() {
        ConnectorProvider http = providers.stream()
                .filter(p -> p.connectorType() == ConnectorType.HTTP).findFirst().orElseThrow();
        String uri = http.targetUri(java.util.Map.of("url", "https://api.example.com/webhook", "method", "POST"));
        assertThat(uri).contains("api.example.com");
    }

    @Test
    void sftpProvider_buildSourceUri_containsHost() {
        ConnectorProvider sftp = providers.stream()
                .filter(p -> p.connectorType() == ConnectorType.SFTP).findFirst().orElseThrow();
        String uri = sftp.sourceUri(java.util.Map.of(
                "host", "sftp.example.com", "username", "user",
                "directory", "/uploads", "password", "secret"));
        assertThat(uri).contains("sftp.example.com").contains("/uploads");
    }

    @Test
    void slackProvider_buildTargetUri_containsWebhookUrl() {
        ConnectorProvider slack = providers.stream()
                .filter(p -> p.connectorType() == ConnectorType.SLACK).findFirst().orElseThrow();
        String uri = slack.targetUri(java.util.Map.of(
                "webhookUrl", "https://hooks.slack.com/services/T00/B00/xxx",
                "channel", "#alerts"));
        assertThat(uri).contains("hooks.slack.com");
    }
}
