package com.platform.form.form.repository;

import com.platform.form.form.domain.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {
    List<FormSubmission> findByFormDefinitionIdAndTenantIdAndIdGreaterThanOrderByIdAsc(
            UUID formDefinitionId, String tenantId, UUID cursor);
    List<FormSubmission> findByFormDefinitionIdAndTenantIdOrderByIdAsc(UUID formDefinitionId, String tenantId);
}
