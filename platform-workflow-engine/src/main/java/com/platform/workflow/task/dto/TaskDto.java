package com.platform.workflow.task.dto;

import java.time.Instant;
import java.util.Map;

public record TaskDto(
        String id,
        String name,
        String description,
        String assignee,
        String owner,
        String processInstanceId,
        String processDefinitionId,
        String formKey,
        String tenantId,
        Instant createTime,
        Instant dueDate,
        Instant claimTime,
        int priority,
        boolean suspended,
        Map<String, Object> variables
) {}
