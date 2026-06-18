package com.platform.workflow.process;

import com.platform.common.web.StandardResponseEnvelope;
import com.platform.workflow.process.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    @PostMapping
    public ResponseEntity<StandardResponseEnvelope<ProcessStartResponse>> startProcess(
            @Valid @RequestBody ProcessStartRequest request,
            HttpServletRequest httpRequest) {
        ProcessStartResponse response = processService.startProcess(request);
        return ResponseEntity.accepted()
                .body(StandardResponseEnvelope.of(response, requestId(httpRequest), MDC.get("traceId")));
    }

    @GetMapping("/{trackingId}/status")
    public ResponseEntity<StandardResponseEnvelope<ProcessStatusResponse>> getStatus(
            @PathVariable String trackingId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(StandardResponseEnvelope.of(
                processService.getStatus(trackingId), requestId(httpRequest), MDC.get("traceId")));
    }

    @GetMapping
    public ResponseEntity<StandardResponseEnvelope<com.platform.common.web.CursorPage<ProcessInstanceDto>>> listProcesses(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(StandardResponseEnvelope.of(
                processService.listProcessInstances(cursor, pageSize), requestId(httpRequest), MDC.get("traceId")));
    }

    @GetMapping("/{processInstanceId}/variables")
    public ResponseEntity<StandardResponseEnvelope<java.util.Map<String, Object>>> getVariables(
            @PathVariable String processInstanceId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(StandardResponseEnvelope.of(
                processService.getProcessVariables(processInstanceId), requestId(httpRequest), MDC.get("traceId")));
    }

    private String requestId(HttpServletRequest req) {
        Object id = req.getAttribute("requestId");
        return id != null ? id.toString() : MDC.get("requestId");
    }
}
