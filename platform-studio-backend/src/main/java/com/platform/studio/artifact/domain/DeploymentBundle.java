package com.platform.studio.artifact.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "deployment_bundles", schema = "platform_meta")
public class DeploymentBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Semantic version for this bundle */
    @Column(nullable = false)
    private String version;

    /** Map of artifactId → version pinned in this bundle */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "artifact_versions", columnDefinition = "jsonb")
    private Map<String, String> artifactVersions;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BundleStatus status = BundleStatus.DRAFT;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deployed_at")
    private Instant deployedAt;

    @Column(name = "deploy_error", columnDefinition = "text")
    private String deployError;
}
