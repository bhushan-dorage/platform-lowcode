package com.platform.webhook.delivery;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "webhook_delivery_logs")
@Data
@NoArgsConstructor
public class WebhookDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String webhookId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String url;

    private Integer httpStatusCode;

    @Column(nullable = false)
    private boolean success = false;

    private String errorMessage;

    @Column(nullable = false)
    private int attemptNumber;

    @Column(nullable = false)
    private Instant attemptedAt = Instant.now();

    private Long durationMs;
}
