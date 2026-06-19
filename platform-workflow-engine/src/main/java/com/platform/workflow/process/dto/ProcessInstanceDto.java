package com.platform.workflow.process.dto;

import java.time.Instant;
import java.util.Map;

public record ProcessInstanceDto(
        String id,
        String processDefinitionId,
        String processKey,
        String businessKey,
        String tenantId,
        boolean ended,
        Instant startTime,
        Instant endTime,
        Map<String, Object> variables
) {}
