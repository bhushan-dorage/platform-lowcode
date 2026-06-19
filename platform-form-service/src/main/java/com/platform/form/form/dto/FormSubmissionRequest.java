package com.platform.form.form.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record FormSubmissionRequest(
        @NotNull Map<String, Object> data,
        String taskId,
        String processInstanceId
) {}
