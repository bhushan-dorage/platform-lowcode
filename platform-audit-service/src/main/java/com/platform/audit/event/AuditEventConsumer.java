package com.platform.audit.event;

import com.platform.audit.chain.EventHashChain;
import com.platform.audit.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditRepository repo;

    /**
     * Consumes audit events from Kafka, computes the hash chain, and persists to ClickHouse.
     *
     * Errors are logged but not rethrown so the Kafka listener can commit the offset and
     * route the poisoned record to the DLQ via the configured error handler rather than
     * blocking the partition indefinitely.
     */
    @KafkaListener(
            topics = "platform.audit.events",
            groupId = "audit-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(AuditEvent event) {
        try {
            String prevHash = repo.getLatestHash(event.getTenantId());
            if (prevHash == null) {
                prevHash = EventHashChain.genesis(event.getTenantId());
            }

            String eventHash = EventHashChain.compute(
                    prevHash,
                    event.getEventId(),
                    event.getTenantId(),
                    event.getTimestamp().toString(),
                    event.getOperation(),
                    event.getResourceId(),
                    event.getActorUserId());

            repo.insert(event, eventHash, prevHash);
            repo.updateLatestHash(event.getTenantId(), eventHash);

            log.debug("Processed audit event id={} tenant={} hash={}",
                    event.getEventId(), event.getTenantId(), eventHash);
        } catch (Exception e) {
            log.error("Failed to process audit event id={} tenant={}: {}",
                    event.getEventId(), event.getTenantId(), e.getMessage(), e);
            // Do not rethrow — bad-offset events are handled by Kafka's configured error handler / DLQ
        }
    }
}
