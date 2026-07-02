package com.platform.page.page.repository;

import com.platform.page.page.domain.PageDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageRepository extends JpaRepository<PageDefinition, UUID> {

    Optional<PageDefinition> findByTenantIdAndPageKey(String tenantId, String pageKey);

    List<PageDefinition> findAllByTenantId(String tenantId);

    List<PageDefinition> findAllByTenantIdAndIdGreaterThanOrderByIdAsc(String tenantId, UUID cursor);

    List<PageDefinition> findAllByTenantIdOrderByIdAsc(String tenantId);

    boolean existsByTenantIdAndPageKey(String tenantId, String pageKey);
}
