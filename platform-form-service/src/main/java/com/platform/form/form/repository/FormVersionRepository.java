package com.platform.form.form.repository;

import com.platform.form.form.domain.FormVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormVersionRepository extends JpaRepository<FormVersion, UUID> {
    List<FormVersion> findByFormDefinitionIdAndTenantIdOrderByVersionDesc(UUID formDefinitionId, String tenantId);
    Optional<FormVersion> findByFormDefinitionIdAndTenantIdAndVersion(UUID formId, String tenantId, int version);
}
