package com.platform.audit.query;

import com.platform.audit.repository.AuditRepository;
import com.platform.common.web.StandardResponseEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditQueryController {

    private final AuditRepository repo;

    /**
     * Queries audit events for a tenant with optional time-range, domain, and operation filters.
     *
     * Results are capped at 1 000 rows server-side regardless of the requested limit.
     * Use the SIEM export endpoint for bulk exports.
     */
    @GetMapping("/events")
    public ResponseEntity<StandardResponseEnvelope<List<Map<String, Object>>>> queryEvents(
            @RequestParam String tenantId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String operation,
            @RequestParam(defaultValue = "100") int limit,
            HttpServletRequest http) {

        List<Map<String, Object>> events = repo.query(tenantId, from, to, domain, operation, limit);
        return ResponseEntity.ok(StandardResponseEnvelope.of(events, MDC.get("requestId"), MDC.get("traceId")));
    }
}
