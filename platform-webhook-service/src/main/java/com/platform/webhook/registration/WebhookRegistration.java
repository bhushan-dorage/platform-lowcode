package com.platform.webhook.registration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "webhook_registrations")
@Data
@NoArgsConstructor
public class WebhookRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String tenantId;

    @NotBlank
    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String secret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webhook_event_types", joinColumns = @JoinColumn(name = "webhook_id"))
    @Column(name = "event_type")
    private List<String> eventTypes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Column(nullable = false)
    private String createdBy;
}
