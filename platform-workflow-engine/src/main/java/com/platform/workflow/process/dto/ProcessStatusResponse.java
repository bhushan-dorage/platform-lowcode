package com.platform.workflow.process.dto;

import java.time.Instant;

public record ProcessStatusResponse(
        String trackingId,
        String processInstanceId,
        String status,
        String processKey,
        String businessKey,
        Instant queuedAt,
        Instant startedAt,
        String errorMessage
) {}
