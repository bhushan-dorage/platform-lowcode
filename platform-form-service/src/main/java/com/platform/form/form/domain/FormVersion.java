package com.platform.form.form.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "form_versions")
public class FormVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_definition_id", nullable = false)
    private UUID formDefinitionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private int version;

    @Column(name = "json_schema", nullable = false, columnDefinition = "jsonb")
    private String jsonSchema;

    @Column(name = "ui_schema", columnDefinition = "jsonb")
    private String uiSchema;

    @Column(name = "published_by")
    private String publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;
}
