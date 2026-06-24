package com.platform.integration.connector;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectorCatalog {

    private static final List<ConnectorDefinition> CATALOG = List.of(
            new ConnectorDefinition(
                    ConnectorType.HTTP,
                    "HTTP/REST",
                    "Call any HTTP/REST endpoint",
                    List.of(
                            new ConnectorDefinition.ConnectorParam("url", "string", "Target URL"),
                            new ConnectorDefinition.ConnectorParam("method", "enum[GET,POST,PUT,DELETE]", "HTTP method")),
                    List.of(
                            new ConnectorDefinition.ConnectorParam("headers", "map", "Additional HTTP headers"),
                            new ConnectorDefinition.ConnectorParam("timeout", "integer", "Timeout in milliseconds"))),

            new ConnectorDefinition(
                    ConnectorType.JDBC,
                    "Database",
                    "Execute SQL queries or stored procedures",
                    List.of(
                            new ConnectorDefinition.ConnectorParam("dataSourceName", "string", "Registered DataSource bean name"),
                            new ConnectorDefinition.ConnectorParam("query", "string", "SQL query to execute")),
                    List.of(
                            new ConnectorDefinition.ConnectorParam("outputType", "enum[SelectList,SelectOne]", "Query output type"))),

            new ConnectorDefinition(
                    ConnectorType.SFTP,
                    "SFTP",
                    "Transfer files via SFTP",
                    List.of(
                            new ConnectorDefinition.ConnectorParam("host", "string", "SFTP server hostname"),
                            new ConnectorDefinition.ConnectorParam("username", "string", "SFTP username"),
                            new ConnectorDefinition.ConnectorParam("directory", "string", "Remote directory path")),
                    List.of(
                            new ConnectorDefinition.ConnectorParam("port", "integer", "SFTP port (default 22)"),
                            new ConnectorDefinition.ConnectorParam("passiveMode", "boolean", "Use passive mode"))),

            new ConnectorDefinition(
                    ConnectorType.EMAIL,
                    "Email",
                    "Send email notifications via SMTP",
                    List.of(
                            new ConnectorDefinition.ConnectorParam("to", "string", "Recipient email address"),
                            new ConnectorDefinition.ConnectorParam("subject", "string", "Email subject")),
                    List.of(
                            new ConnectorDefinition.ConnectorParam("cc", "string", "CC recipients"),
                            new ConnectorDefinition.ConnectorParam("contentType", "string", "Content type (default text/plain)"))),

            new ConnectorDefinition(
                    ConnectorType.SLACK,
                    "Slack",
                    "Post messages to Slack channels",
                    List.of(
                            new ConnectorDefinition.ConnectorParam("webhookUrl", "string", "Slack incoming webhook URL"),
                            new ConnectorDefinition.ConnectorParam("channel", "string", "Target Slack channel")),
                    List.of(
                            new ConnectorDefinition.ConnectorParam("username", "string", "Bot username"),
                            new ConnectorDefinition.ConnectorParam("iconEmoji", "string", "Bot icon emoji")))
    );

    private final Map<ConnectorType, ConnectorDefinition> index =
            CATALOG.stream().collect(Collectors.toMap(ConnectorDefinition::type, Function.identity()));

    public List<ConnectorDefinition> listAll() {
        return CATALOG;
    }

    public Optional<ConnectorDefinition> findByType(ConnectorType type) {
        return Optional.ofNullable(index.get(type));
    }
}
