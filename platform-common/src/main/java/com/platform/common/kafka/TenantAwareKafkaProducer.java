package com.platform.common.kafka;

import com.platform.common.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Tenant-aware Kafka producer that:
 *  1. Prefixes the topic with the current tenant's Kafka prefix.
 *  2. Injects W3C traceparent/tracestate headers so consumers can continue the trace.
 *  3. Records send latency via Micrometer.
 */
@Component
public class TenantAwareKafkaProducer {

    // W3C Trace Context header names (https://www.w3.org/TR/trace-context/)
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String TRACESTATE_HEADER = "tracestate";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public TenantAwareKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                    MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Sends a message to "{tenantKafkaPrefix}{topicSuffix}".
     *
     * @param topicSuffix the logical topic name without tenant prefix (e.g. "workflow.created")
     * @param key         the partition key
     * @param payload     the message payload (serialised as JSON by producer config)
     */
    public CompletableFuture<SendResult<String, Object>> send(String topicSuffix,
                                                              String key,
                                                              Object payload) {
        String tenantId = TenantContext.getTenantId();
        String topic = TenantContext.getKafkaPrefix() + topicSuffix;

        Timer.Sample sample = Timer.start(meterRegistry);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        injectTraceContext(record);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            String outcome = (ex == null) ? "success" : "failure";
            sample.stop(Timer.builder("kafka.producer.send")
                    .tag("topic", topicSuffix)
                    .tag("tenantId", tenantId)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        });

        return future;
    }

    /**
     * Propagates the active W3C trace context into Kafka headers so downstream consumers
     * can link their spans to the originating trace without out-of-band correlation ids.
     */
    private void injectTraceContext(ProducerRecord<String, Object> record) {
        Span currentSpan = Span.current();
        if (currentSpan == null) {
            return;
        }
        SpanContext spanCtx = currentSpan.getSpanContext();
        if (!spanCtx.isValid()) {
            return;
        }

        // traceparent format: 00-{traceId}-{spanId}-{flags}
        String traceFlags = spanCtx.getTraceFlags().asHex();
        String traceparent = "00-" + spanCtx.getTraceId() + "-" + spanCtx.getSpanId() + "-" + traceFlags;
        record.headers().add(new RecordHeader(TRACEPARENT_HEADER,
                traceparent.getBytes(StandardCharsets.UTF_8)));

        String traceState = spanCtx.getTraceState().asMap()
                .entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        if (!traceState.isEmpty()) {
            record.headers().add(new RecordHeader(TRACESTATE_HEADER,
                    traceState.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
