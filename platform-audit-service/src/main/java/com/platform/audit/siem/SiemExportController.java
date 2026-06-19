package com.platform.audit.siem;

import com.platform.audit.repository.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
public class SiemExportController {

    private final AuditRepository repo;
    private final SiemExportService siemService;

    /**
     * Exports audit events for a tenant in the requested SIEM format.
     *
     * The response is plain text (one event per line) so it can be piped directly
     * into a SIEM ingestion tool such as ArcSight SmartConnector, QRadar Log Source,
     * or Splunk Universal Forwarder.
     *
     * Limit is capped at 1 000 by the repository layer.
     */
    @GetMapping("/export/siem")
    public ResponseEntity<String> exportSiem(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "CEF") SiemFormat format,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1000") int limit) {

        List<Map<String, Object>> events = repo.query(tenantId, from, to, null, null, limit);
        String output = siemService.formatBatch(events, format);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(output);
    }
}
