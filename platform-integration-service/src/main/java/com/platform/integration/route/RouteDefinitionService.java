package com.platform.integration.route;

import com.platform.integration.engine.CamelRouteEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteDefinitionService {

    private final RouteDefinitionRepository repository;
    private final CamelRouteEngine engine;

    public List<RouteDefinitionEntity> listForTenant(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public RouteDefinitionEntity getForTenant(String id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
    }

    @Transactional
    public RouteDefinitionEntity create(RouteDefinitionEntity def) {
        def.setStatus(RouteDefinitionEntity.RouteStatus.STOPPED);
        def.setCreatedAt(Instant.now());
        return repository.save(def);
    }

    @Transactional
    public RouteDefinitionEntity update(String id, String tenantId, RouteDefinitionEntity updated) {
        RouteDefinitionEntity existing = getForTenant(id, tenantId);
        boolean wasRunning = existing.getStatus() == RouteDefinitionEntity.RouteStatus.RUNNING;
        if (wasRunning) {
            engine.stopRoute(existing);
        }
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setRouteDefinition(updated.getRouteDefinition());
        existing.setSourceConnectorType(updated.getSourceConnectorType());
        existing.setTargetConnectorType(updated.getTargetConnectorType());
        existing.setUpdatedAt(Instant.now());
        existing.setStatus(RouteDefinitionEntity.RouteStatus.STOPPED);
        RouteDefinitionEntity saved = repository.save(existing);
        if (wasRunning) {
            startRoute(id, tenantId);
        }
        return saved;
    }

    @Transactional
    public RouteDefinitionEntity startRoute(String id, String tenantId) {
        RouteDefinitionEntity def = getForTenant(id, tenantId);
        engine.startRoute(def);
        def.setStatus(RouteDefinitionEntity.RouteStatus.RUNNING);
        def.setUpdatedAt(Instant.now());
        return repository.save(def);
    }

    @Transactional
    public RouteDefinitionEntity stopRoute(String id, String tenantId) {
        RouteDefinitionEntity def = getForTenant(id, tenantId);
        engine.stopRoute(def);
        def.setStatus(RouteDefinitionEntity.RouteStatus.STOPPED);
        def.setUpdatedAt(Instant.now());
        return repository.save(def);
    }

    @Transactional
    public void delete(String id, String tenantId) {
        RouteDefinitionEntity def = getForTenant(id, tenantId);
        if (def.getStatus() == RouteDefinitionEntity.RouteStatus.RUNNING) {
            engine.stopRoute(def);
        }
        repository.delete(def);
    }
}
