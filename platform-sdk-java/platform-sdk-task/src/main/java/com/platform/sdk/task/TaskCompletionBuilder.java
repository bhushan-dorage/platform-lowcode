package com.platform.sdk.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.sdk.core.http.PlatformHttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TaskCompletionBuilder {

    private final PlatformHttpClient http;
    private final String taskId;
    private String outcome;
    private final Map<String, Object> variables = new HashMap<>();

    TaskCompletionBuilder(PlatformHttpClient http, String taskId) {
        this.http = http;
        this.taskId = taskId;
    }

    public TaskCompletionBuilder outcome(String outcome) {
        this.outcome = outcome;
        return this;
    }

    public TaskCompletionBuilder variable(String name, Object value) {
        this.variables.put(name, value);
        return this;
    }

    public TaskCompletionBuilder formData(Map<String, Object> data) {
        this.variables.putAll(data);
        return this;
    }

    public void submit() throws IOException {
        Map<String, Object> body = new HashMap<>();
        if (outcome != null) body.put("outcome", outcome);
        if (!variables.isEmpty()) body.put("variables", variables);
        http.post("/v1/tasks/" + taskId + "/complete", body, new TypeReference<Void>() {});
    }
}
