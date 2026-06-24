package com.platform.integration.engine;

import com.platform.integration.route.RouteDefinitionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CamelRouteEngine {

    private final CamelContext camelContext;

    public void startRoute(RouteDefinitionEntity def) {
        String routeId = routeId(def);
        try {
            if (camelContext.getRoute(routeId) != null) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
            }
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("timer://" + routeId + "?period=60000&delay=60000")
                            .routeId(routeId)
                            .log("Integration route [" + def.getName() + "] tick for tenant [" + def.getTenantId() + "]");
                }
            });
            camelContext.getRouteController().startRoute(routeId);
            log.info("Started Camel route {} for tenant {}", routeId, def.getTenantId());
        } catch (Exception e) {
            log.error("Failed to start Camel route {} for tenant {}: {}", routeId, def.getTenantId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to start route: " + def.getName(), e);
        }
    }

    public void stopRoute(RouteDefinitionEntity def) {
        String routeId = routeId(def);
        try {
            if (camelContext.getRoute(routeId) != null) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
                log.info("Stopped Camel route {} for tenant {}", routeId, def.getTenantId());
            }
        } catch (Exception e) {
            log.error("Failed to stop Camel route {} for tenant {}: {}", routeId, def.getTenantId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to stop route: " + def.getName(), e);
        }
    }

    public boolean isRunning(String routeId) {
        var route = camelContext.getRoute(routeId);
        return route != null && camelContext.getRouteController().getRouteStatus(routeId).isStarted();
    }

    public static String routeId(RouteDefinitionEntity def) {
        return "tenant-" + def.getTenantId() + "-route-" + def.getId();
    }
}
