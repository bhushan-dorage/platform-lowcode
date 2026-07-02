package com.platform.audit.siem;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SiemExportServiceTest {

    private final SiemExportService service = new SiemExportService();

    private Map<String, Object> sampleEvent() {
        return Map.of(
                "event_id", "evt-001",
                "event_type", "RESOURCE_CREATED",
                "domain", "workflow",
                "tenant_id", "acme",
                "timestamp", "2024-06-01T12:00:00Z",
                "actor_user_id", "alice",
                "actor_ip", "10.0.0.1",
                "resource_type", "Process",
                "resource_id", "proc-42",
                "operation", "CREATE"
        );
    }

    @Test
    void format_cef_containsRequiredFields() {
        String result = service.format(sampleEvent(), SiemFormat.CEF);
        assertThat(result)
                .startsWith("CEF:0|Platform|AuditService|1.0|")
                .contains("RESOURCE_CREATED")
                .contains("suser=alice")
                .contains("src=10.0.0.1")
                .contains("act=CREATE")
                .contains("cs1=acme");
    }

    @Test
    void format_leef_containsRequiredFields() {
        String result = service.format(sampleEvent(), SiemFormat.LEEF);
        assertThat(result)
                .startsWith("LEEF:2.0|Platform|AuditService|1.0|")
                .contains("RESOURCE_CREATED")
                .contains("usrName=alice")
                .contains("src=10.0.0.1")
                .contains("op=CREATE");
    }

    @Test
    void format_json_isValidJson() {
        String result = service.format(sampleEvent(), SiemFormat.JSON);
        assertThat(result)
                .startsWith("{")
                .endsWith("}")
                .contains("evt-001");
    }

    @Test
    void formatBatch_joinsWithNewline() {
        Map<String, Object> e = sampleEvent();
        String result = service.formatBatch(List.of(e, e), SiemFormat.CEF);
        assertThat(result.split("\n")).hasSize(2);
    }

    @Test
    void formatBatch_emptyList_returnsEmpty() {
        String result = service.formatBatch(List.of(), SiemFormat.CEF);
        assertThat(result).isEmpty();
    }
}
