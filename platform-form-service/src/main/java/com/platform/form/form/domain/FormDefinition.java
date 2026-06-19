package com.platform.form.form.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "form_definitions")
public class FormDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "form_key", nullable = false)
    private String formKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "current_version", nullable = false)
    private int currentVersion = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormStatus status = FormStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
