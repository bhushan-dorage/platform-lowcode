package com.platform.integration.route;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RouteDefinitionRepository extends JpaRepository<RouteDefinitionEntity, String> {
    List<RouteDefinitionEntity> findByTenantId(String tenantId);
    Optional<RouteDefinitionEntity> findByIdAndTenantId(String id, String tenantId);
    void deleteByIdAndTenantId(String id, String tenantId);
}
