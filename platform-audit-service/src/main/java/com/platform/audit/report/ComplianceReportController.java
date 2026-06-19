package com.platform.audit.report;

import com.platform.common.web.StandardResponseEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class ComplianceReportController {

    private final ComplianceReportService reportService;

    /**
     * Generates a compliance evidence report for the given framework and tenant.
     *
     * The report includes a summary of audit activity broken down by operation, domain,
     * and event type, plus a framework-specific list of controls with evidence counts.
     *
     * The {@code to} parameter defaults to the current instant when omitted so callers
     * can request "from date X to now" without knowing the current server time.
     */
    @GetMapping("/reports/{framework}")
    public ResponseEntity<StandardResponseEnvelope<Map<String, Object>>> getReport(
            @PathVariable ComplianceFramework framework,
            @RequestParam String tenantId,
            @RequestParam(required = false, defaultValue = "2024-01-01T00:00:00Z") String from,
            @RequestParam(required = false) String to,
            HttpServletRequest http) {

        if (to == null) {
            to = Instant.now().toString();
        }

        Map<String, Object> report = reportService.generate(framework, tenantId, from, to);
        return ResponseEntity.ok(
                StandardResponseEnvelope.of(report, MDC.get("requestId"), MDC.get("traceId")));
    }
}
