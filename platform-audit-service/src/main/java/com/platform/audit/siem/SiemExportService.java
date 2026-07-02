package com.platform.audit.siem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SiemExportService {

    /**
     * Formats a single audit event row into the requested SIEM format.
     *
     * CEF  — ArcSight Common Event Format v0. Uses the most common CEF extension fields
     *         (rt, suser, src, cs1/cs2/cs3 with Labels, act).
     * LEEF — IBM QRadar Log Event Extended Format 2.0. Fields are tab-delimited.
     * JSON — Raw JSON serialisation of the event map.
     */
    public String format(Map<String, Object> event, SiemFormat format) {
        return switch (format) {
            case CEF -> "CEF:0|Platform|AuditService|1.0|"
                    + event.get("event_type") + "|"
                    + event.get("event_type") + "|5|"
                    + "rt=" + event.get("timestamp")
                    + " suser=" + event.get("actor_user_id")
                    + " src=" + event.get("actor_ip")
                    + " cs1=" + event.get("tenant_id")
                    + " cs1Label=tenantId"
                    + " cs2=" + event.get("resource_type")
                    + " cs2Label=resourceType"
                    + " cs3=" + event.get("resource_id")
                    + " cs3Label=resourceId"
                    + " act=" + event.get("operation");

            case LEEF -> "LEEF:2.0|Platform|AuditService|1.0|"
                    + event.get("event_type") + "|"
                    + "cat=" + event.get("domain")
                    + "\tusrName=" + event.get("actor_user_id")
                    + "\tsrc=" + event.get("actor_ip")
                    + "\tresource=" + event.get("resource_id")
                    + "\top=" + event.get("operation");

            case JSON -> {
                try {
                    yield new ObjectMapper().writeValueAsString(event);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize audit event to JSON", e);
                }
            }
        };
    }

    /**
     * Formats a batch of audit event rows, one per line.
     *
     * The output is suitable for streaming directly into a SIEM ingestion pipeline
     * or writing to a log file.
     */
    public String formatBatch(List<Map<String, Object>> events, SiemFormat format) {
        return events.stream()
                .map(event -> format(event, format))
                .collect(Collectors.joining("\n"));
    }
}
