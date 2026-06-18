package com.platform.workflow.history.dto;

import java.time.Instant;

public record ProcessAnalyticsDto(
        long totalStarted,
        long totalCompleted,
        long totalActive,
        double avgDurationMs,
        String tenantId,
        Instant from,
        Instant to
) {}
