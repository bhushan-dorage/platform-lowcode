package com.platform.form.form.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "form_submissions")
public class FormSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_definition_id", nullable = false)
    private UUID formDefinitionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "form_version", nullable = false)
    private int formVersion;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

    @Column(nullable = false, columnDefinition = "jsonb")
    private String data;
}
