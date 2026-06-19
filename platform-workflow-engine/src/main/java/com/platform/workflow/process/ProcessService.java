package com.platform.workflow.process;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.web.CursorPage;
import com.platform.workflow.exception.ResourceNotFoundException;
import com.platform.workflow.process.dto.*;
import com.platform.workflow.process.messaging.ProcessStartEvent;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessService {

    private final TenantAwareKafkaProducer kafkaProducer;
    private final ProcessTracker processTracker;
    private final HistoryService historyService;
    private final ClaimCheckService claimCheckService;

    @Timed(value = "workflow.process.start")
    public ProcessStartResponse startProcess(ProcessStartRequest req) {
        String trackingId = UUID.randomUUID().toString();
        String tenantId = TenantContext.getTenantId();

        ProcessStartEvent event = new ProcessStartEvent(
                trackingId,
                tenantId,
                TenantContext.getTier().name(),
                req.processKey(),
                req.businessKey(),
                req.variables(),
                req.startedBy(),
                Instant.now()
        );

        processTracker.record(tenantId, trackingId, event);
        kafkaProducer.send("process.events", trackingId, event);
        log.info("Process start queued processKey={} trackingId={}", req.processKey(), trackingId);
        return new ProcessStartResponse(trackingId, "QUEUED", Instant.now());
    }

    @Timed(value = "workflow.process.status")
    public ProcessStatusResponse getStatus(String trackingId) {
        ProcessStatusResponse status = processTracker.getStatus(TenantContext.getTenantId(), trackingId);
        if (status == null) throw new ResourceNotFoundException("Tracking ID not found: " + trackingId);
        return status;
    }

    @Timed(value = "workflow.process.list")
    public CursorPage<ProcessInstanceDto> listProcessInstances(String cursor, int pageSize) {
        String tenantId = TenantContext.getTenantId();
        var query = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .orderByProcessInstanceId().asc();

        List<HistoricProcessInstance> results = query.listPage(0, pageSize + 1);

        // Apply cursor filter in-memory — Flowable's API doesn't expose id-greater-than
        if (cursor != null) {
            results = results.stream()
                    .filter(p -> p.getId().compareTo(cursor) > 0)
                    .toList();
        }

        boolean hasMore = results.size() > pageSize;
        List<HistoricProcessInstance> page = hasMore ? results.subList(0, pageSize) : results;
        String nextCursor = hasMore ? page.get(page.size() - 1).getId() : null;

        List<ProcessInstanceDto> dtos = page.stream().map(this::toDto).toList();
        return CursorPage.of(dtos, nextCursor, hasMore, pageSize);
    }

    @Timed(value = "workflow.process.variables")
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        String tenantId = TenantContext.getTenantId();
        var instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .processInstanceTenantId(tenantId)
                .singleResult();
        if (instance == null) throw new ResourceNotFoundException("Process instance not found: " + processInstanceId);

        Map<String, Object> raw = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        v -> v.getVariableName(),
                        v -> v.getValue() != null ? v.getValue() : ""
                ));
        return claimCheckService.resolveVariables(tenantId, raw);
    }

    private ProcessInstanceDto toDto(HistoricProcessInstance p) {
        return new ProcessInstanceDto(
                p.getId(),
                p.getProcessDefinitionId(),
                p.getProcessDefinitionKey(),
                p.getBusinessKey(),
                p.getTenantId(),
                p.getEndTime() != null,
                p.getStartTime() != null ? p.getStartTime().toInstant() : null,
                p.getEndTime() != null ? p.getEndTime().toInstant() : null,
                Map.of()
        );
    }
}
