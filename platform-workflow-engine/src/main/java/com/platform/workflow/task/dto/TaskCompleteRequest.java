package com.platform.workflow.task.dto;

import java.util.Map;

public record TaskCompleteRequest(
        Map<String, Object> variables,
        String comment
) {
    public TaskCompleteRequest {
        if (variables == null) variables = Map.of();
    }
}
