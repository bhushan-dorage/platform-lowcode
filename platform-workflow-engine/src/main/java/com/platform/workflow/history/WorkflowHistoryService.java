package com.platform.workflow.history;

import com.platform.common.tenant.TenantContext;
import com.platform.workflow.exception.ResourceNotFoundException;
import com.platform.workflow.history.dto.HistoricActivityDto;
import com.platform.workflow.history.dto.ProcessAnalyticsDto;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowHistoryService {

    private final HistoryService historyService;

    @Timed(name = "workflow.history.activities")
    public List<HistoricActivityDto> getProcessHistory(String processInstanceId) {
        String tenantId = TenantContext.getTenantId();
        // Verify instance belongs to this tenant
        var instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .processInstanceTenantId(tenantId)
                .singleResult();
        if (instance == null) throw new ResourceNotFoundException("Process instance not found: " + processInstanceId);

        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Timed(name = "workflow.analytics.processes")
    public ProcessAnalyticsDto getProcessAnalytics(Instant from, Instant to) {
        String tenantId = TenantContext.getTenantId();
        HistoricProcessInstanceQuery base = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .startedAfter(java.util.Date.from(from))
                .startedBefore(java.util.Date.from(to));

        long totalStarted = base.count();
        long totalCompleted = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .startedAfter(java.util.Date.from(from))
                .startedBefore(java.util.Date.from(to))
                .finished().count();
        long totalActive = totalStarted - totalCompleted;

        double avgDurationMs = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .startedAfter(java.util.Date.from(from))
                .startedBefore(java.util.Date.from(to))
                .finished().list().stream()
                .filter(p -> p.getDurationInMillis() != null)
                .mapToLong(p -> p.getDurationInMillis())
                .average().orElse(0.0);

        return new ProcessAnalyticsDto(totalStarted, totalCompleted, totalActive, avgDurationMs, tenantId, from, to);
    }

    private HistoricActivityDto toDto(HistoricActivityInstance a) {
        return new HistoricActivityDto(
                a.getId(), a.getActivityId(), a.getActivityName(), a.getActivityType(),
                a.getProcessInstanceId(), a.getTenantId(),
                a.getStartTime() != null ? a.getStartTime().toInstant() : null,
                a.getEndTime() != null ? a.getEndTime().toInstant() : null,
                a.getDurationInMillis(), a.getAssignee()
        );
    }
}
