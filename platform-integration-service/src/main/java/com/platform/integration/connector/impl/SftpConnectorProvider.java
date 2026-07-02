package com.platform.integration.connector.impl;

import com.platform.integration.connector.ConnectorDefinition;
import com.platform.integration.connector.ConnectorDefinition.ConnectorParam;
import com.platform.integration.connector.ConnectorType;
import com.platform.integration.connector.spi.ConnectorProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SFTP connector backed by the Camel {@code camel-ftp} component.
 *
 * <p>As a <b>source</b>: polls a remote SFTP directory for new files.
 * Files are read, passed through the route, and left on the server
 * ({@code noop=true}) unless {@code delete=true} is configured.
 *
 * <p>As a <b>target</b>: writes the route body to the configured SFTP directory.
 *
 * <p><b>Security note:</b> passwords should be supplied via Vault-backed
 * environment variables and referenced as {@code config.get("password")}.
 * Never store credentials in the {@code routeDefinition} JSON at rest.
 */
@Component
public class SftpConnectorProvider implements ConnectorProvider {

    private static final ConnectorDefinition DEFINITION = new ConnectorDefinition(
            ConnectorType.SFTP,
            "SFTP",
            "Transfer files to/from a remote SFTP server",
            List.of(
                    new ConnectorParam("host", "string", "SFTP server hostname or IP"),
                    new ConnectorParam("username", "string", "SFTP username"),
                    new ConnectorParam("directory", "string", "Remote directory path")),
            List.of(
                    new ConnectorParam("port", "integer", "SFTP port (default 22)"),
                    new ConnectorParam("pollDelaySeconds", "integer", "Poll interval in seconds when used as source (default 60)"),
                    new ConnectorParam("delete", "boolean", "Delete remote file after processing (default false)"),
                    new ConnectorParam("fileName", "string", "Fixed filename when writing (target only)")));

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.SFTP;
    }

    @Override
    public ConnectorDefinition definition() {
        return DEFINITION;
    }

    @Override
    public String sourceUri(Map<String, Object> config) {
        int port = Integer.parseInt(config.getOrDefault("port", "22").toString());
        long delayMs = Long.parseLong(config.getOrDefault("pollDelaySeconds", "60").toString()) * 1000L;
        boolean delete = Boolean.parseBoolean(config.getOrDefault("delete", "false").toString());
        return "sftp://"
                + requireString(config, "username") + "@"
                + requireString(config, "host") + ":" + port + "/"
                + requireString(config, "directory")
                + "?password=RAW(" + requireString(config, "password") + ")"
                + "&delay=" + delayMs
                + "&noop=" + !delete
                + "&delete=" + delete
                + "&stepwise=false";
    }

    @Override
    public String targetUri(Map<String, Object> config) {
        int port = Integer.parseInt(config.getOrDefault("port", "22").toString());
        StringBuilder uri = new StringBuilder("sftp://")
                .append(requireString(config, "username")).append("@")
                .append(requireString(config, "host")).append(":").append(port).append("/")
                .append(requireString(config, "directory"))
                .append("?password=RAW(").append(requireString(config, "password")).append(")")
                .append("&stepwise=false");
        if (config.containsKey("fileName")) {
            uri.append("&fileName=").append(config.get("fileName"));
        }
        return uri.toString();
    }

    private static String requireString(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("SFTP connector requires '" + key + "'");
        }
        return val.toString();
    }
}
