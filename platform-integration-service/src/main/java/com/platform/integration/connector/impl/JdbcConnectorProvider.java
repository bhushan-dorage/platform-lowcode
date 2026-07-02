package com.platform.integration.connector.impl;

import com.platform.integration.connector.ConnectorDefinition;
import com.platform.integration.connector.ConnectorDefinition.ConnectorParam;
import com.platform.integration.connector.ConnectorType;
import com.platform.integration.connector.spi.ConnectorProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * JDBC/SQL connector.
 *
 * <p>As a <b>source</b>: polls a SQL SELECT statement using the Camel
 * {@code sql} component, which supports consumer mode with configurable delay.
 *
 * <p>As a <b>target</b>: executes the SQL statement in the route body against
 * the named DataSource using the Camel {@code jdbc} component.
 */
@Component
public class JdbcConnectorProvider implements ConnectorProvider {

    private static final ConnectorDefinition DEFINITION = new ConnectorDefinition(
            ConnectorType.JDBC,
            "Database",
            "Execute SQL queries or poll a database table for new rows",
            List.of(
                    new ConnectorParam("dataSourceName", "string", "Registered Spring DataSource bean name"),
                    new ConnectorParam("query", "string", "SQL SELECT (source) or INSERT/UPDATE template (target)")),
            List.of(
                    new ConnectorParam("outputType", "enum[SelectList,SelectOne]", "Query output type for SELECT (default SelectList)"),
                    new ConnectorParam("pollDelaySeconds", "integer", "Poll interval in seconds when used as source (default 30)")));

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.JDBC;
    }

    @Override
    public ConnectorDefinition definition() {
        return DEFINITION;
    }

    /**
     * SQL consumer: polls the SELECT query and emits one message per row.
     * Uses {@code camel-sql-starter} which supports consumer mode.
     */
    @Override
    public String sourceUri(Map<String, Object> config) {
        String query = requireString(config, "query");
        String dsName = requireString(config, "dataSourceName");
        long delayMs = Long.parseLong(config.getOrDefault("pollDelaySeconds", "30").toString()) * 1000L;
        String outputType = config.getOrDefault("outputType", "SelectList").toString();
        return "sql:" + query
                + "?dataSource=#" + dsName
                + "&outputType=" + outputType
                + "&consumer.delay=" + delayMs;
    }

    /**
     * JDBC producer: executes the SQL in the route body against the DataSource.
     * The message body should be the SQL statement or a {@code Map} of named params.
     */
    @Override
    public String targetUri(Map<String, Object> config) {
        String dsName = requireString(config, "dataSourceName");
        return "jdbc:" + dsName;
    }

    private static String requireString(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("JDBC connector requires '" + key + "'");
        }
        return val.toString();
    }
}
