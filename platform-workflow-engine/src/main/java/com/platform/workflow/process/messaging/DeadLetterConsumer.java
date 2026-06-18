package com.platform.workflow.process.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topicPattern = ".*\\.deadletter",
            groupId = "workflow-engine-dead-letter"
    )
    public void consume(@Payload DeadLetterEvent event) {
        log.error("Dead letter received tenantId={} trackingId={} error={}",
                event.originalEvent().tenantId(),
                event.originalEvent().trackingId(),
                event.errorMessage());

        Counter.builder("workflow.dead_letter.received")
                .tag("tenantId", event.originalEvent().tenantId())
                .tag("eventType", "process.start")
                .register(meterRegistry)
                .increment();
    }
}
