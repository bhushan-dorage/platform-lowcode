package com.platform.studio.artifact.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "studio_artifacts", schema = "platform_meta",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "type", "name"}))
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ArtifactType type;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    private String description;

    /** Semantic version of latest published artifact */
    @Column(name = "current_version")
    private String currentVersion;

    /** Git commit SHA of latest commit (draft or published) */
    @Column(name = "head_commit_sha")
    private String headCommitSha;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ArtifactStatus status = ArtifactStatus.DRAFT;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
