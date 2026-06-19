package com.platform.audit.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditEvent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String eventId;
    private String eventType;
    private String domain;
    private String tenantId;
    private Instant timestamp;
    private String actorUserId;
    private List<String> actorRoles;
    private String actorIp;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String operation;
    private String prevState;
    private String newState;
    private String sourceService;
    private String requestId;
    private String traceId;
    private Map<String, String> metadata;

    /**
     * Serializes the metadata map to a JSON string.
     * Returns "{}" when metadata is null or serialization fails.
     */
    public String getMetadataAsJson() {
        if (metadata == null) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
