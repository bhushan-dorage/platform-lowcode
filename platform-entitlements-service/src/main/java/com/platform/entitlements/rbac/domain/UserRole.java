package com.platform.entitlements.rbac.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "user_roles", schema = "platform_meta")
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "assigned_at", updatable = false)
    private Instant assignedAt = Instant.now();
}
