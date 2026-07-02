package com.platform.integration.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.integration.connector.ConnectorType;
import com.platform.integration.connector.spi.ConnectorProvider;
import com.platform.integration.connector.spi.ConnectorProviderRegistry;
import com.platform.integration.route.RouteDefinitionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CamelRouteEngine {

    private final CamelContext camelContext;
    private final ConnectorProviderRegistry registry;
    private final ObjectMapper objectMapper;

    public void startRoute(RouteDefinitionEntity def) {
        String routeId = routeId(def);
        try {
            if (camelContext.getRoute(routeId) != null) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
            }

            ConnectorProvider sourceProvider = registry.get(ConnectorType.valueOf(def.getSourceConnectorType()));
            ConnectorProvider targetProvider = registry.get(ConnectorType.valueOf(def.getTargetConnectorType()));
            RouteConfig config = RouteConfig.parse(objectMapper, def.getRouteDefinition());

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    RouteDefinition route = from(sourceProvider.sourceUri(config.source()))
                            .routeId(routeId);

                    route.onException(Exception.class)
                            .handled(true)
                            .log("Route [" + routeId + "] error on attempt ${exchangeProperty.CamelRedeliveryCounter}: ${exception.message}")
                            .maximumRedeliveries(3)
                            .redeliveryDelay(5000)
                            .end();

                    for (String uri : sourceProvider.intermediateUris(config.source())) {
                        route.to(uri);
                    }

                    route.to(targetProvider.targetUri(config.target()));
                }
            });

            camelContext.getRouteController().startRoute(routeId);
            log.info("Started route {} [{} → {}] for tenant {}",
                    routeId, def.getSourceConnectorType(), def.getTargetConnectorType(), def.getTenantId());

        } catch (Exception e) {
            log.error("Failed to start route {} for tenant {}: {}", routeId, def.getTenantId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to start route: " + def.getName(), e);
        }
    }

    public void stopRoute(RouteDefinitionEntity def) {
        String routeId = routeId(def);
        try {
            if (camelContext.getRoute(routeId) != null) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
                log.info("Stopped route {} for tenant {}", routeId, def.getTenantId());
            }
        } catch (Exception e) {
            log.error("Failed to stop route {} for tenant {}: {}", routeId, def.getTenantId(), e.getMessage(), e);
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
