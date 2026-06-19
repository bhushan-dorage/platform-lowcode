package com.platform.workflow.task;

import com.platform.common.web.CursorPage;
import com.platform.common.web.StandardResponseEnvelope;
import com.platform.workflow.task.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/inbox")
    public ResponseEntity<StandardResponseEnvelope<CursorPage<TaskDto>>> getInbox(
            Authentication auth,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest req) {
        return ok(taskService.getInbox(auth.getName(), cursor, pageSize), req);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<StandardResponseEnvelope<TaskDto>> getTask(
            @PathVariable String taskId, HttpServletRequest req) {
        return ok(taskService.getTask(taskId), req);
    }

    @PostMapping("/{taskId}/claim")
    public ResponseEntity<StandardResponseEnvelope<Void>> claimTask(
            @PathVariable String taskId, Authentication auth, HttpServletRequest req) {
        taskService.claimTask(taskId, auth.getName());
        return ok(null, req);
    }

    @PostMapping("/{taskId}/unclaim")
    public ResponseEntity<StandardResponseEnvelope<Void>> unclaimTask(
            @PathVariable String taskId, Authentication auth, HttpServletRequest req) {
        taskService.unclaimTask(taskId, auth.getName());
        return ok(null, req);
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<StandardResponseEnvelope<Void>> completeTask(
            @PathVariable String taskId,
            @RequestBody TaskCompleteRequest body,
            Authentication auth,
            HttpServletRequest req) {
        taskService.completeTask(taskId, auth.getName(), body.variables(), body.comment());
        return ok(null, req);
    }

    @PostMapping("/{taskId}/assign")
    public ResponseEntity<StandardResponseEnvelope<Void>> assignTask(
            @PathVariable String taskId,
            @Valid @RequestBody TaskAssignRequest body,
            HttpServletRequest req) {
        taskService.assignTask(taskId, body.assignee());
        return ok(null, req);
    }

    @PostMapping("/{taskId}/escalate")
    public ResponseEntity<StandardResponseEnvelope<Void>> escalateTask(
            @PathVariable String taskId,
            @Valid @RequestBody TaskEscalateRequest body,
            HttpServletRequest req) {
        taskService.escalateTask(taskId, body.escalateTo(), body.reason());
        return ok(null, req);
    }

    private <T> ResponseEntity<StandardResponseEnvelope<T>> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return ResponseEntity.ok(StandardResponseEnvelope.of(
                data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId")));
    }
}
