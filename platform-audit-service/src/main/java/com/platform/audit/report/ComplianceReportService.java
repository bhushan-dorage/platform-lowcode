package com.platform.audit.report;

import com.platform.audit.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceReportService {

    private final AuditRepository repo;

    /**
     * Generates a compliance evidence report for the given framework, tenant, and time period.
     *
     * Events are fetched from ClickHouse (up to 10 000 rows) and summarised by operation,
     * domain, and event type. A framework-specific control mapping is then produced so that
     * auditors can cross-reference platform activity against individual control requirements.
     *
     * Note: the per-control eventCount is currently set to the total event count as a
     * simplified demonstration. A production implementation would filter by the specific
     * operations or domains relevant to each control.
     */
    public Map<String, Object> generate(ComplianceFramework framework,
                                        String tenantId,
                                        String from,
                                        String to) {
        List<Map<String, Object>> events = repo.query(tenantId, from, to, null, null, 10000);

        Map<String, Long> byOperation = events.stream()
                .collect(groupingBy(e -> String.valueOf(e.get("operation")), counting()));

        Map<String, Long> byDomain = events.stream()
                .collect(groupingBy(e -> String.valueOf(e.get("domain")), counting()));

        Map<String, Long> byEventType = events.stream()
                .collect(groupingBy(e -> String.valueOf(e.get("event_type")), counting()));

        List<Map<String, Object>> controls = buildControls(framework, (long) events.size());

        return Map.of(
                "framework", framework.name(),
                "tenantId", tenantId,
                "period", Map.of("from", from, "to", to),
                "summary", Map.of(
                        "totalEvents", events.size(),
                        "byOperation", byOperation,
                        "byDomain", byDomain,
                        "byEventType", byEventType),
                "controls", controls);
    }

    private List<Map<String, Object>> buildControls(ComplianceFramework framework, long totalEventCount) {
        return switch (framework) {
            case SOC2 -> List.of(
                    control("CC6.1", "Logical Access Controls", totalEventCount),
                    control("CC6.2", "New User Provisioning", totalEventCount),
                    control("CC6.3", "Role Modification", totalEventCount),
                    control("CC7.2", "Security Events", totalEventCount),
                    control("CC7.3", "System Incidents", totalEventCount));

            case ISO27001 -> List.of(
                    control("A.9.1", "Access Control Policy", totalEventCount),
                    control("A.9.2", "User Access Management", totalEventCount),
                    control("A.12.4", "Logging and Monitoring", totalEventCount),
                    control("A.16.1", "Information Security Incidents", totalEventCount));

            case GDPR -> List.of(
                    control("Art.5", "Data Processing Principles", totalEventCount),
                    control("Art.17", "Right to Erasure", totalEventCount),
                    control("Art.30", "Records of Processing Activities", totalEventCount));

            case HIPAA -> List.of(
                    control("164.312(a)", "Access Control", totalEventCount),
                    control("164.312(b)", "Audit Controls", totalEventCount),
                    control("164.312(d)", "Person Authentication", totalEventCount));
        };
    }

    private Map<String, Object> control(String id, String name, long eventCount) {
        return Map.of("id", id, "name", name, "eventCount", eventCount);
    }
}
