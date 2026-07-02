package com.platform.integration.connector.impl;

import com.platform.integration.connector.ConnectorDefinition;
import com.platform.integration.connector.ConnectorDefinition.ConnectorParam;
import com.platform.integration.connector.ConnectorType;
import com.platform.integration.connector.spi.ConnectorProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Slack connector. Delivers messages to Slack via the Incoming Webhooks API.
 *
 * <p>Slack webhooks are plain HTTP POSTs, so this connector reuses the
 * Camel {@code camel-http} component. The route body must be a JSON string
 * conforming to the Slack webhook payload format, e.g.:
 * <pre>{"text": "Hello from Platform", "channel": "#alerts"}</pre>
 *
 * <p>Slack does not expose a consumer API that this connector supports.
 * When used as a <b>source</b> a scheduler stub fires for configuration
 * testing purposes only — real inbound Slack events require the Events API
 * and a dedicated HTTP listener.
 */
@Component
public class SlackConnectorProvider implements ConnectorProvider {

    private static final ConnectorDefinition DEFINITION = new ConnectorDefinition(
            ConnectorType.SLACK,
            "Slack",
            "Post messages to Slack channels via Incoming Webhooks",
            List.of(
                    new ConnectorParam("webhookUrl", "string", "Slack Incoming Webhook URL"),
                    new ConnectorParam("channel", "string", "Target channel (e.g. #alerts)")),
            List.of(
                    new ConnectorParam("username", "string", "Bot display name (overrides webhook default)"),
                    new ConnectorParam("iconEmoji", "string", "Bot icon emoji (e.g. :robot_face:)")));

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.SLACK;
    }

    @Override
    public ConnectorDefinition definition() {
        return DEFINITION;
    }

    /**
     * Slack has no native polling consumer — returns a low-frequency scheduler
     * stub that can be used to push periodic summaries to Slack.
     */
    @Override
    public String sourceUri(Map<String, Object> config) {
        return "scheduler://slack-poll?delay=0&period=300000";
    }

    /**
     * Posts the route body (must be a Slack-compatible JSON string) to the
     * webhook URL via HTTP POST. The {@code camel-http} component handles
     * the HTTPS transport.
     */
    @Override
    public String targetUri(Map<String, Object> config) {
        String webhookUrl = requireString(config, "webhookUrl");
        return webhookUrl
                + (webhookUrl.contains("?") ? "&" : "?")
                + "httpMethod=POST"
                + "&Content-Type=application/json"
                + "&throwExceptionOnFailure=true";
    }

    private static String requireString(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("Slack connector requires '" + key + "'");
        }
        return val.toString();
    }
}
