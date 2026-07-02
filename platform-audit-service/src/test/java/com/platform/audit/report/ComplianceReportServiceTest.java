package com.platform.audit.report;

import com.platform.audit.repository.AuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceReportServiceTest {

    @Mock AuditRepository repo;
    @InjectMocks ComplianceReportService reportService;

    private List<Map<String, Object>> sampleEvents() {
        return List.of(
                Map.of("event_type", "PERMISSION_GRANTED", "domain", "entitlements",
                        "operation", "GRANT", "tenant_id", "acme"),
                Map.of("event_type", "RESOURCE_CREATED", "domain", "workflow",
                        "operation", "CREATE", "tenant_id", "acme"),
                Map.of("event_type", "USER_LOGIN", "domain", "auth",
                        "operation", "LOGIN", "tenant_id", "acme")
        );
    }

    @Test
    void generate_soc2_returnsCorrectStructure() {
        when(repo.query(eq("acme"), any(), any(), isNull(), isNull(), eq(10000)))
                .thenReturn(sampleEvents());

        Map<String, Object> report = reportService.generate(
                ComplianceFramework.SOC2, "acme",
                "2024-01-01T00:00:00Z", "2024-12-31T23:59:59Z");

        assertThat(report).containsKey("framework");
        assertThat(report.get("framework")).isEqualTo("SOC2");
        assertThat(report).containsKey("summary");
        assertThat(report).containsKey("controls");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertThat(summary.get("totalEvents")).isEqualTo(3);
    }

    @Test
    void generate_gdpr_hasGdprControls() {
        when(repo.query(any(), any(), any(), isNull(), isNull(), eq(10000)))
                .thenReturn(sampleEvents());

        Map<String, Object> report = reportService.generate(
                ComplianceFramework.GDPR, "acme",
                "2024-01-01T00:00:00Z", "2024-12-31T23:59:59Z");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls = (List<Map<String, Object>>) report.get("controls");
        assertThat(controls).isNotEmpty();
        assertThat(controls.get(0)).containsKey("id");
        assertThat(controls.get(0).get("id").toString()).startsWith("Art.");
    }

    @Test
    void generate_iso27001_hasIsoControls() {
        when(repo.query(any(), any(), any(), isNull(), isNull(), eq(10000)))
                .thenReturn(sampleEvents());

        Map<String, Object> report = reportService.generate(
                ComplianceFramework.ISO27001, "acme",
                "2024-01-01T00:00:00Z", "2024-12-31T23:59:59Z");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls = (List<Map<String, Object>>) report.get("controls");
        assertThat(controls.get(0).get("id").toString()).startsWith("A.");
    }

    @Test
    void generate_hipaa_hasHipaaControls() {
        when(repo.query(any(), any(), any(), isNull(), isNull(), eq(10000)))
                .thenReturn(sampleEvents());

        Map<String, Object> report = reportService.generate(
                ComplianceFramework.HIPAA, "acme",
                "2024-01-01T00:00:00Z", "2024-12-31T23:59:59Z");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls = (List<Map<String, Object>>) report.get("controls");
        assertThat(controls).isNotEmpty();
    }

    @Test
    void generate_emptyEvents_zeroTotal() {
        when(repo.query(any(), any(), any(), isNull(), isNull(), eq(10000)))
                .thenReturn(List.of());

        Map<String, Object> report = reportService.generate(
                ComplianceFramework.SOC2, "acme",
                "2024-01-01T00:00:00Z", "2024-12-31T23:59:59Z");

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertThat(summary.get("totalEvents")).isEqualTo(0);
    }
}
