package com.platform.integration.route;

import com.platform.integration.engine.CamelRouteEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteDefinitionServiceTest {

    @Mock
    private RouteDefinitionRepository repository;

    @Mock
    private CamelRouteEngine engine;

    @InjectMocks
    private RouteDefinitionService service;

    private RouteDefinitionEntity sampleRoute;

    @BeforeEach
    void setUp() {
        sampleRoute = new RouteDefinitionEntity();
        sampleRoute.setId("route-1");
        sampleRoute.setTenantId("acme");
        sampleRoute.setName("Test Route");
        sampleRoute.setSourceConnectorType("HTTP");
        sampleRoute.setTargetConnectorType("SLACK");
        sampleRoute.setRouteDefinition("{\"steps\": []}");
        sampleRoute.setStatus(RouteDefinitionEntity.RouteStatus.STOPPED);
        sampleRoute.setCreatedBy("user-1");
    }

    @Test
    void create_setsStatusToStopped() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        RouteDefinitionEntity result = service.create(sampleRoute);
        assertThat(result.getStatus()).isEqualTo(RouteDefinitionEntity.RouteStatus.STOPPED);
    }

    @Test
    void listForTenant_delegatesToRepository() {
        when(repository.findByTenantId("acme")).thenReturn(List.of(sampleRoute));
        assertThat(service.listForTenant("acme")).containsExactly(sampleRoute);
    }

    @Test
    void getForTenant_throwsWhenNotFound() {
        when(repository.findByIdAndTenantId("missing", "acme")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getForTenant("missing", "acme"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Route not found");
    }

    @Test
    void startRoute_setsStatusToRunning() {
        when(repository.findByIdAndTenantId("route-1", "acme")).thenReturn(Optional.of(sampleRoute));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        RouteDefinitionEntity result = service.startRoute("route-1", "acme");
        assertThat(result.getStatus()).isEqualTo(RouteDefinitionEntity.RouteStatus.RUNNING);
        verify(engine).startRoute(sampleRoute);
    }

    @Test
    void stopRoute_setsStatusToStopped() {
        sampleRoute.setStatus(RouteDefinitionEntity.RouteStatus.RUNNING);
        when(repository.findByIdAndTenantId("route-1", "acme")).thenReturn(Optional.of(sampleRoute));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        RouteDefinitionEntity result = service.stopRoute("route-1", "acme");
        assertThat(result.getStatus()).isEqualTo(RouteDefinitionEntity.RouteStatus.STOPPED);
        verify(engine).stopRoute(sampleRoute);
    }

    @Test
    void delete_stopsRunningRouteFirst() {
        sampleRoute.setStatus(RouteDefinitionEntity.RouteStatus.RUNNING);
        when(repository.findByIdAndTenantId("route-1", "acme")).thenReturn(Optional.of(sampleRoute));
        service.delete("route-1", "acme");
        verify(engine).stopRoute(sampleRoute);
        verify(repository).delete(sampleRoute);
    }
}
