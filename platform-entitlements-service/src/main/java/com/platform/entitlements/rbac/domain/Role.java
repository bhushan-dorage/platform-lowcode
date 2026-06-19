package com.platform.entitlements.rbac.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "roles", schema = "platform_meta")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "parent_role_id")
    private UUID parentRoleId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            schema = "platform_meta",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
