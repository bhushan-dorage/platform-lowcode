package com.platform.sdk.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.sdk.core.http.PlatformHttpClient;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class TaskClient {

    private final PlatformHttpClient http;

    public TaskQuery inbox() {
        return new TaskQuery(http);
    }

    public Task claim(String taskId) throws IOException {
        return http.post("/v1/tasks/" + taskId + "/claim", Map.of(), new TypeReference<>() {});
    }

    public TaskCompletionBuilder complete(String taskId) {
        return new TaskCompletionBuilder(http, taskId);
    }

    public void reassign(String taskId, String assignTo, String reason) throws IOException {
        http.post("/v1/tasks/" + taskId + "/reassign",
                Map.of("assignTo", assignTo, "reason", reason), new TypeReference<Void>() {});
    }
}
