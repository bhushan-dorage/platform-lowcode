package com.platform.entitlements.abac.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "data_policies", schema = "platform_meta")
public class DataPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicyOperation operation;

    @Column(name = "policy_yaml", nullable = false, columnDefinition = "text")
    private String policyYaml;

    /** Compiled SQL WHERE predicate — populated at activation time, NOT at query time */
    @Column(name = "compiled_predicate", columnDefinition = "text")
    private String compiledPredicate;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
