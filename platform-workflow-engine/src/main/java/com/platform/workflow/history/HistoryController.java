package com.platform.workflow.history;

import com.platform.common.web.StandardResponseEnvelope;
import com.platform.workflow.history.dto.HistoricActivityDto;
import com.platform.workflow.history.dto.ProcessAnalyticsDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class HistoryController {

    private final WorkflowHistoryService historyService;

    @GetMapping("/api/v1/processes/{processInstanceId}/history")
    public ResponseEntity<StandardResponseEnvelope<List<HistoricActivityDto>>> getHistory(
            @PathVariable String processInstanceId,
            HttpServletRequest req) {
        return ok(historyService.getProcessHistory(processInstanceId), req);
    }

    @GetMapping("/api/v1/analytics/processes")
    public ResponseEntity<StandardResponseEnvelope<ProcessAnalyticsDto>> getAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest req) {
        return ok(historyService.getProcessAnalytics(from, to), req);
    }

    private <T> ResponseEntity<StandardResponseEnvelope<T>> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return ResponseEntity.ok(StandardResponseEnvelope.of(
                data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId")));
    }
}
