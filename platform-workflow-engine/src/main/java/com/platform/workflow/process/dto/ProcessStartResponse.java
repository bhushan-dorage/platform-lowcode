package com.platform.workflow.process.dto;

import java.time.Instant;

public record ProcessStartResponse(
        String trackingId,
        String status,
        Instant queuedAt
) {}
