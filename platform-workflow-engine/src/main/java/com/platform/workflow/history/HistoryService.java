package com.platform.workflow.history;

import com.platform.common.tenant.TenantContext;
import com.platform.workflow.history.dto.HistoricActivityDto;
import com.platform.workflow.history.dto.ProcessAnalyticsDto;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HistoryService {

    private final org.flowable.engine.HistoryService flowableHistoryService;

    public HistoryService(
            @Qualifier("flowableHistoryService") org.flowable.engine.HistoryService flowableHistoryService) {
        this.flowableHistoryService = flowableHistoryService;
    }

    @Timed(value = "workflow.history.process")
    public List<HistoricActivityDto> getProcessHistory(String processInstanceId) {
        String tenantId = TenantContext.getTenantId();
        Span.current().setAttribute("tenant.id", tenantId);

        List<HistoricActivityInstance> activities = flowableHistoryService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        return activities.stream()
                .filter(a -> tenantId.equals(a.getTenantId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Timed(value = "workflow.history.analytics")
    public ProcessAnalyticsDto getProcessAnalytics(Instant from, Instant to) {
        String tenantId = TenantContext.getTenantId();
        Span.current().setAttribute("tenant.id", tenantId);

        Date fromDate = Date.from(from);
        Date toDate = Date.from(to);

        long totalStarted = flowableHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .startedAfter(fromDate)
                .startedBefore(toDate)
                .count();

        List<HistoricProcessInstance> completed = flowableHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .startedAfter(fromDate)
                .startedBefore(toDate)
                .finished()
                .list();

        long totalCompleted = completed.size();

        long totalActive = flowableHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .startedAfter(fromDate)
                .startedBefore(toDate)
                .unfinished()
                .count();

        double avgDurationMs = completed.stream()
                .filter(p -> p.getDurationInMillis() != null)
                .mapToLong(HistoricProcessInstance::getDurationInMillis)
                .average()
                .orElse(0.0);

        return new ProcessAnalyticsDto(totalStarted, totalCompleted, totalActive, avgDurationMs, tenantId, from, to);
    }

    private HistoricActivityDto toDto(HistoricActivityInstance a) {
        Instant startTime = a.getStartTime() != null ? a.getStartTime().toInstant() : null;
        Instant endTime = a.getEndTime() != null ? a.getEndTime().toInstant() : null;
        return new HistoricActivityDto(
                a.getId(),
                a.getActivityId(),
                a.getActivityName(),
                a.getActivityType(),
                a.getProcessInstanceId(),
                a.getTenantId(),
                startTime,
                endTime,
                a.getDurationInMillis(),
                a.getAssignee()
        );
    }
}
