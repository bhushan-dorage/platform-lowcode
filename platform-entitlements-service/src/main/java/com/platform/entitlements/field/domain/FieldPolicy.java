package com.platform.entitlements.field.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "field_policies", schema = "platform_meta")
public class FieldPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private FieldAccessLevel accessLevel;

    /** SpEL expression for CONDITIONAL access */
    @Column(name = "condition_expr")
    private String conditionExpr;

    @Enumerated(EnumType.STRING)
    @Column(name = "mask_pattern")
    private MaskPattern maskPattern;
}
