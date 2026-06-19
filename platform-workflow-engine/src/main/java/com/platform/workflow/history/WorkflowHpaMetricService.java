package com.platform.workflow.history;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ManagementService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowHpaMetricService implements ApplicationRunner {

    private final MeterRegistry meterRegistry;
    private final ManagementService managementService;

    @Override
    public void run(ApplicationArguments args) {
        // Exposed via /actuator/prometheus — used as K8s HPA custom metric
        Gauge.builder("workflow.active.jobs", this, WorkflowHpaMetricService::getActiveJobCount)
                .description("Active Flowable async jobs — used by K8s HPA")
                .register(meterRegistry);
        log.info("Registered workflow.active.jobs HPA metric");
    }

    private double getActiveJobCount() {
        try {
            return managementService.createJobQuery().count();
        } catch (Exception e) {
            log.warn("Failed to query active job count", e);
            return 0;
        }
    }
}
