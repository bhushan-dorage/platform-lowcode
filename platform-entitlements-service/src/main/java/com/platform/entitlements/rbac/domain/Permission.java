package com.platform.entitlements.rbac.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "permissions", schema = "platform_meta")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Format: platform:{resource}:{action} */
    @Column(nullable = false, unique = true)
    private String name;

    private String description;
}
