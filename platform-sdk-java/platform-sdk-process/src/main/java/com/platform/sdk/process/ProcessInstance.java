package com.platform.sdk.process;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class ProcessInstance {
    private String id;
    private String processKey;
    private String businessKey;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private String tenantId;
    private Map<String, Object> variables;
}
