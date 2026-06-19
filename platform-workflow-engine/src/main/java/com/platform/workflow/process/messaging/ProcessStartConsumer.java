package com.platform.workflow.process.messaging;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantRegistry;
import com.platform.workflow.process.ClaimCheckService;
import com.platform.workflow.process.ProcessTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessStartConsumer {

    private final RuntimeService runtimeService;
    private final ProcessTracker processTracker;
    private final ClaimCheckService claimCheckService;
    private final TenantAwareKafkaProducer kafkaProducer;
    private final TenantRegistry tenantRegistry;

    // max.poll.records=20 configured in KafkaConsumerConfig for burst absorption
    @KafkaListener(
            topicPattern = ".*\\.process\\.events",
            groupId = "workflow-engine-process-start",
            containerFactory = "processStartListenerContainerFactory"
    )
    public void consume(@Payload ProcessStartEvent event, Acknowledgment ack) {
        try {
            TenantContext.set(event.tenantId(), tenantRegistry.resolveTier(event.tenantId()));
            var resolvedVars = claimCheckService.resolveVariables(event.tenantId(), event.variables());

            var instance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(event.processKey())
                    .tenantId(event.tenantId())       // CRITICAL: always set tenantId
                    .businessKey(event.businessKey())
                    .variables(resolvedVars)
                    .start();

            processTracker.markStarted(event.tenantId(), event.trackingId(), instance.getId());
            log.info("Process started processKey={} instanceId={} tenantId={}",
                    event.processKey(), instance.getId(), event.tenantId());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to start process trackingId={} tenantId={}", event.trackingId(), event.tenantId(), ex);
            processTracker.markFailed(event.tenantId(), event.trackingId(), ex.getMessage());
            publishToDeadLetter(event, ex);
            ack.acknowledge(); // ack to avoid reprocessing a bad message indefinitely
        } finally {
            TenantContext.clear();
        }
    }

    private void publishToDeadLetter(ProcessStartEvent event, Exception ex) {
        try {
            TenantContext.set(event.tenantId(), tenantRegistry.resolveTier(event.tenantId()));
            kafkaProducer.send("deadletter", event.trackingId(), new DeadLetterEvent(event, ex.getMessage()));
        } catch (Exception e) {
            log.error("Failed to publish dead letter for trackingId={}", event.trackingId(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
