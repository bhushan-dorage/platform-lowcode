package com.platform.workflow.history.dto;

import java.time.Instant;

public record HistoricActivityDto(
        String id,
        String activityId,
        String activityName,
        String activityType,
        String processInstanceId,
        String tenantId,
        Instant startTime,
        Instant endTime,
        Long durationInMillis,
        String assignee
) {}
