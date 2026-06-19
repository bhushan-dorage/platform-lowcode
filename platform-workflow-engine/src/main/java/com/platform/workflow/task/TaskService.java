package com.platform.workflow.task;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.web.CursorPage;
import com.platform.workflow.exception.ResourceNotFoundException;
import com.platform.workflow.process.ClaimCheckService;
import com.platform.workflow.task.dto.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final TaskClaimService taskClaimService;
    private final ClaimCheckService claimCheckService;
    private final TenantAwareKafkaProducer kafkaProducer;

    public TaskService(
            @Qualifier("flowableTaskService") org.flowable.engine.TaskService flowableTaskService,
            TaskClaimService taskClaimService,
            ClaimCheckService claimCheckService,
            TenantAwareKafkaProducer kafkaProducer) {
        this.flowableTaskService = flowableTaskService;
        this.taskClaimService = taskClaimService;
        this.claimCheckService = claimCheckService;
        this.kafkaProducer = kafkaProducer;
    }

    @Timed(value = "workflow.task.inbox")
    public CursorPage<TaskDto> getInbox(String userId, String cursor, int pageSize) {
        String tenantId = TenantContext.getTenantId();
        // Fetch pageSize+1 to determine hasMore; cursor filters by id > lastSeen
        var query = flowableTaskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .taskTenantId(tenantId)        // CRITICAL: always filter by tenant
                .orderByTaskId().asc();

        List<Task> results = query.listPage(0, pageSize + 1);

        if (cursor != null) {
            results = results.stream()
                    .filter(t -> t.getId().compareTo(cursor) > 0)
                    .toList();
        }

        boolean hasMore = results.size() > pageSize;
        List<Task> page = hasMore ? results.subList(0, pageSize) : results;
        String nextCursor = hasMore ? page.get(page.size() - 1).getId() : null;
        return CursorPage.of(page.stream().map(this::toDto).toList(), nextCursor, hasMore, pageSize);
    }

    @Timed(value = "workflow.task.get")
    public TaskDto getTask(String taskId) {
        Task task = requireTask(taskId);
        return toDto(task);
    }

    @Timed(value = "workflow.task.claim")
    public void claimTask(String taskId, String userId) {
        requireTask(taskId);
        taskClaimService.claimWithLock(taskId, userId, () -> flowableTaskService.claim(taskId, userId));
        log.info("Task claimed taskId={} userId={}", taskId, userId);
    }

    @Timed(value = "workflow.task.unclaim")
    public void unclaimTask(String taskId, String userId) {
        requireTask(taskId);
        flowableTaskService.unclaim(taskId);
    }

    @Timed(value = "workflow.task.complete")
    public void completeTask(String taskId, String userId, Map<String, Object> variables, String comment) {
        requireTask(taskId);
        Map<String, Object> prepared = claimCheckService.prepareVariables(TenantContext.getTenantId(), variables);
        if (comment != null && !comment.isBlank()) {
            flowableTaskService.addComment(taskId, null, comment);
        }
        flowableTaskService.complete(taskId, prepared);
        log.info("Task completed taskId={} userId={}", taskId, userId);
    }

    @Timed(value = "workflow.task.assign")
    public void assignTask(String taskId, String assignee) {
        requireTask(taskId);
        flowableTaskService.setAssignee(taskId, assignee);
    }

    @Timed(value = "workflow.task.escalate")
    public void escalateTask(String taskId, String escalateTo, String reason) {
        Task task = requireTask(taskId);
        if (reason != null && !reason.isBlank()) {
            flowableTaskService.addComment(taskId, task.getProcessInstanceId(), "ESCALATED: " + reason);
        }
        flowableTaskService.setAssignee(taskId, escalateTo);
        kafkaProducer.send("task.events", taskId, Map.of(
                "eventType", "TASK_ESCALATED",
                "taskId", taskId,
                "escalateTo", escalateTo,
                "reason", reason != null ? reason : "",
                "tenantId", TenantContext.getTenantId(),
                "timestamp", Instant.now().toString()
        ));
        log.info("Task escalated taskId={} escalateTo={}", taskId, escalateTo);
    }

    private Task requireTask(String taskId) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .taskTenantId(TenantContext.getTenantId())  // CRITICAL: tenant isolation
                .singleResult();
        if (task == null) throw new ResourceNotFoundException("Task not found: " + taskId);
        return task;
    }

    private TaskDto toDto(Task t) {
        return new TaskDto(
                t.getId(), t.getName(), t.getDescription(), t.getAssignee(), t.getOwner(),
                t.getProcessInstanceId(), t.getProcessDefinitionId(), t.getFormKey(),
                t.getTenantId(),
                t.getCreateTime() != null ? t.getCreateTime().toInstant() : null,
                t.getDueDate() != null ? t.getDueDate().toInstant() : null,
                t.getClaimTime() != null ? t.getClaimTime().toInstant() : null,
                t.getPriority(), t.isSuspended(), Map.of()
        );
    }
}
