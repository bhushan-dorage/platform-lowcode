package com.platform.form.form.repository;

import com.platform.form.form.domain.FormDefinition;
import com.platform.form.form.domain.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {
    Optional<FormDefinition> findByTenantIdAndFormKey(String tenantId, String formKey);
    // Cursor pagination: id > lastSeen, ordered by id
    List<FormDefinition> findByTenantIdAndIdGreaterThanOrderByIdAsc(String tenantId, UUID cursor);
    List<FormDefinition> findByTenantIdOrderByIdAsc(String tenantId);
    boolean existsByTenantIdAndFormKey(String tenantId, String formKey);
}
