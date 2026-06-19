package com.platform.workflow.process.messaging;

import java.time.Instant;
import java.util.Map;

public record ProcessStartEvent(
        String trackingId,
        String tenantId,
        String tier,
        String processKey,
        String businessKey,
        Map<String, Object> variables,
        String startedBy,
        Instant requestedAt
) {}
