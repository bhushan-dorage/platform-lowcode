package com.platform.workflow.process.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record ProcessStartRequest(
        @NotBlank String processKey,
        String businessKey,
        Map<String, Object> variables,
        String startedBy
) {
    public ProcessStartRequest {
        if (variables == null) variables = Map.of();
    }
}
