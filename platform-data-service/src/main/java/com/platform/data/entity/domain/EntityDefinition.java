package com.platform.data.entity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "entity_definitions")
public class EntityDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** JSON Schema defining the shape of entity records */
    @Column(nullable = false, columnDefinition = "jsonb")
    private String schema;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
