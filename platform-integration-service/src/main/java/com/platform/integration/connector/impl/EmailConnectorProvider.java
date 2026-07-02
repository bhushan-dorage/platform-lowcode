package com.platform.integration.connector.impl;

import com.platform.integration.connector.ConnectorDefinition;
import com.platform.integration.connector.ConnectorDefinition.ConnectorParam;
import com.platform.integration.connector.ConnectorType;
import com.platform.integration.connector.spi.ConnectorProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Email connector backed by the Camel {@code camel-mail} component.
 *
 * <p>As a <b>source</b>: polls an IMAP/POP3 mailbox for new messages.
 *
 * <p>As a <b>target</b>: sends the route body as an email via SMTP.
 * The message body becomes the email body; use Camel headers
 * {@code CamelMailTo}, {@code CamelMailSubject} to override at runtime.
 */
@Component
public class EmailConnectorProvider implements ConnectorProvider {

    private static final ConnectorDefinition DEFINITION = new ConnectorDefinition(
            ConnectorType.EMAIL,
            "Email",
            "Send email notifications or poll a mailbox for incoming messages",
            List.of(
                    new ConnectorParam("smtpHost", "string", "SMTP server hostname (target)"),
                    new ConnectorParam("to", "string", "Recipient address(es), comma-separated (target)"),
                    new ConnectorParam("subject", "string", "Email subject (target)")),
            List.of(
                    new ConnectorParam("smtpPort", "integer", "SMTP port (default 587)"),
                    new ConnectorParam("from", "string", "Sender address (default noreply@platform.local)"),
                    new ConnectorParam("cc", "string", "CC recipients"),
                    new ConnectorParam("contentType", "string", "Content type (default text/plain)"),
                    new ConnectorParam("imapHost", "string", "IMAP server hostname (source)"),
                    new ConnectorParam("imapUsername", "string", "IMAP username (source)"),
                    new ConnectorParam("pollDelaySeconds", "integer", "IMAP poll interval in seconds (default 60)")));

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.EMAIL;
    }

    @Override
    public ConnectorDefinition definition() {
        return DEFINITION;
    }

    /**
     * IMAP consumer: polls a mailbox for new messages, leaving them undeleted.
     */
    @Override
    public String sourceUri(Map<String, Object> config) {
        String host = requireString(config, "imapHost");
        String username = requireString(config, "imapUsername");
        String password = requireString(config, "imapPassword");
        long delayMs = Long.parseLong(config.getOrDefault("pollDelaySeconds", "60").toString()) * 1000L;
        return "imap://" + host
                + "?username=" + username
                + "&password=RAW(" + password + ")"
                + "&delete=false"
                + "&unseen=true"
                + "&consumer.delay=" + delayMs;
    }

    /**
     * SMTP producer: sends the route body as an email.
     */
    @Override
    public String targetUri(Map<String, Object> config) {
        String host = requireString(config, "smtpHost");
        int port = Integer.parseInt(config.getOrDefault("smtpPort", "587").toString());
        String to = requireString(config, "to");
        String subject = requireString(config, "subject");
        String from = config.getOrDefault("from", "noreply@platform.local").toString();
        String contentType = config.getOrDefault("contentType", "text/plain").toString();

        StringBuilder uri = new StringBuilder("smtp://").append(host).append(":").append(port)
                .append("?to=").append(to)
                .append("&subject=").append(encode(subject))
                .append("&from=").append(from)
                .append("&contentType=").append(contentType);

        if (config.containsKey("cc")) {
            uri.append("&CC=").append(config.get("cc"));
        }
        return uri.toString();
    }

    private static String encode(String value) {
        return value.replace(" ", "+").replace("&", "%26");
    }

    private static String requireString(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("Email connector requires '" + key + "'");
        }
        return val.toString();
    }
}
