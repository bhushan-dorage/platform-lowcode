package com.platform.workflow.process.messaging;

import java.time.Instant;

public record DeadLetterEvent(
        ProcessStartEvent originalEvent,
        String errorMessage,
        Instant failedAt
) {
    public DeadLetterEvent(ProcessStartEvent originalEvent, String errorMessage) {
        this(originalEvent, errorMessage, Instant.now());
    }
}
