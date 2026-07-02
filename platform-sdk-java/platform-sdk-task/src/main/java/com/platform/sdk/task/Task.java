package com.platform.sdk.task;

import lombok.Data;
import java.time.Instant;

@Data
public class Task {
    private String id;
    private String name;
    private String processKey;
    private String businessKey;
    private String priority;
    private Instant dueDate;
    private String status;
    private String assignee;
    private String tenantId;
}
