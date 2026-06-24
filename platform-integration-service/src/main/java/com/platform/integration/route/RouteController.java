package com.platform.integration.route;

import com.platform.integration.connector.ConnectorCatalog;
import com.platform.integration.connector.ConnectorDefinition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RouteController {

    private final RouteDefinitionService routeService;
    private final ConnectorCatalog connectorCatalog;

    @GetMapping("/api/v1/integrations/connectors")
    public List<ConnectorDefinition> listConnectors() {
        return connectorCatalog.listAll();
    }

    @GetMapping("/api/v1/integrations/routes")
    public List<RouteDefinitionEntity> listRoutes(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return routeService.listForTenant(tenantId);
    }

    @PostMapping("/api/v1/integrations/routes")
    public ResponseEntity<RouteDefinitionEntity> createRoute(
            @Valid @RequestBody RouteDefinitionEntity def,
            @AuthenticationPrincipal Jwt jwt) {
        def.setTenantId(jwt.getClaimAsString("tenant_id"));
        def.setCreatedBy(jwt.getSubject());
        RouteDefinitionEntity created = routeService.create(def);
        return ResponseEntity.created(URI.create("/api/v1/integrations/routes/" + created.getId())).body(created);
    }

    @GetMapping("/api/v1/integrations/routes/{id}")
    public RouteDefinitionEntity getRoute(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return routeService.getForTenant(id, jwt.getClaimAsString("tenant_id"));
    }

    @PutMapping("/api/v1/integrations/routes/{id}")
    public RouteDefinitionEntity updateRoute(
            @PathVariable String id,
            @RequestBody RouteDefinitionEntity def,
            @AuthenticationPrincipal Jwt jwt) {
        return routeService.update(id, jwt.getClaimAsString("tenant_id"), def);
    }

    @PostMapping("/api/v1/integrations/routes/{id}/start")
    public RouteDefinitionEntity startRoute(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return routeService.startRoute(id, jwt.getClaimAsString("tenant_id"));
    }

    @PostMapping("/api/v1/integrations/routes/{id}/stop")
    public RouteDefinitionEntity stopRoute(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return routeService.stopRoute(id, jwt.getClaimAsString("tenant_id"));
    }

    @DeleteMapping("/api/v1/integrations/routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        routeService.delete(id, jwt.getClaimAsString("tenant_id"));
        return ResponseEntity.noContent().build();
    }
}
