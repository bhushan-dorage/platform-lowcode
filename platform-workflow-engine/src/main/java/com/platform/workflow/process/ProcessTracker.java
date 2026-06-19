package com.platform.workflow.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.workflow.process.dto.ProcessStatusResponse;
import com.platform.workflow.process.messaging.ProcessStartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessTracker {

    private static final Duration TTL = Duration.ofHours(24);
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void record(String tenantId, String trackingId, ProcessStartEvent event) {
        Map<String, Object> state = new HashMap<>();
        state.put("trackingId", trackingId);
        state.put("status", "QUEUED");
        state.put("processKey", event.processKey());
        state.put("businessKey", event.businessKey());
        state.put("queuedAt", event.requestedAt().toString());
        state.put("processInstanceId", null);
        state.put("startedAt", null);
        state.put("errorMessage", null);
        writeState(tenantId, trackingId, state);
    }

    public void markStarted(String tenantId, String trackingId, String processInstanceId) {
        Map<String, Object> state = readRaw(tenantId, trackingId);
        if (state == null) return;
        state.put("status", "STARTED");
        state.put("processInstanceId", processInstanceId);
        state.put("startedAt", Instant.now().toString());
        writeState(tenantId, trackingId, state);
    }

    public void markFailed(String tenantId, String trackingId, String errorMessage) {
        Map<String, Object> state = readRaw(tenantId, trackingId);
        if (state == null) return;
        state.put("status", "FAILED");
        state.put("errorMessage", errorMessage);
        writeState(tenantId, trackingId, state);
    }

    public ProcessStatusResponse getStatus(String tenantId, String trackingId) {
        Map<String, Object> state = readRaw(tenantId, trackingId);
        if (state == null) return null;
        return new ProcessStatusResponse(
                (String) state.get("trackingId"),
                (String) state.get("processInstanceId"),
                (String) state.get("status"),
                (String) state.get("processKey"),
                (String) state.get("businessKey"),
                parseInstant(state.get("queuedAt")),
                parseInstant(state.get("startedAt")),
                (String) state.get("errorMessage")
        );
    }

    private void writeState(String tenantId, String trackingId, Map<String, Object> state) {
        try {
            String key = redisKey(tenantId, trackingId);
            redis.opsForValue().set(key, objectMapper.writeValueAsString(state), TTL);
        } catch (Exception e) {
            log.error("Failed to write tracking state for trackingId={}", trackingId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRaw(String tenantId, String trackingId) {
        try {
            String json = redis.opsForValue().get(redisKey(tenantId, trackingId));
            if (json == null) return null;
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to read tracking state for trackingId={}", trackingId, e);
            return null;
        }
    }

    private static String redisKey(String tenantId, String trackingId) {
        return tenantId + ":process-track:" + trackingId;
    }

    private static Instant parseInstant(Object val) {
        if (val == null) return null;
        try { return Instant.parse(val.toString()); } catch (Exception e) { return null; }
    }
}
