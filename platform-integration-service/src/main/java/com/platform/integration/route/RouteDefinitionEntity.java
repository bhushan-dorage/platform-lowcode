package com.platform.integration.route;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "integration_routes")
@Data
@NoArgsConstructor
public class RouteDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteStatus status = RouteStatus.STOPPED;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String routeDefinition;

    @Column(nullable = false)
    private String sourceConnectorType;

    @Column(nullable = false)
    private String targetConnectorType;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Column(nullable = false)
    private String createdBy;

    public enum RouteStatus {
        RUNNING, STOPPED, ERROR
    }
}
