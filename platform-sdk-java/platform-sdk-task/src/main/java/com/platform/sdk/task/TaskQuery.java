package com.platform.sdk.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.sdk.core.http.PlatformHttpClient;
import com.platform.sdk.core.pagination.PagedResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TaskQuery {

    private final PlatformHttpClient http;
    private String forGroup;
    private String forProcess;
    private String priority;
    private int pageSize = 20;

    TaskQuery(PlatformHttpClient http) {
        this.http = http;
    }

    public TaskQuery forGroup(String group) { this.forGroup = group; return this; }
    public TaskQuery forProcess(String processKey) { this.forProcess = processKey; return this; }
    public TaskQuery withPriority(String priority) { this.priority = priority; return this; }
    public TaskQuery pageSize(int pageSize) { this.pageSize = pageSize; return this; }

    public PagedResult<Task> fetch() throws IOException {
        return fetchPage(null);
    }

    private PagedResult<Task> fetchPage(String cursor) throws IOException {
        StringBuilder url = new StringBuilder("/v1/tasks?pageSize=").append(pageSize);
        if (forGroup != null) url.append("&group=").append(forGroup);
        if (forProcess != null) url.append("&processKey=").append(forProcess);
        if (priority != null) url.append("&priority=").append(priority);
        if (cursor != null) url.append("&cursor=").append(cursor);

        Map<String, Object> response = http.get(url.toString(), new TypeReference<>() {});
        List<Task> content = parseContent(response);
        String nextCursor = (String) response.get("cursor");
        boolean hasMore = Boolean.TRUE.equals(response.get("hasMore"));
        return new PagedResult<>(content, nextCursor, hasMore, c -> {
            try { return fetchPage(c); } catch (IOException e) { throw new RuntimeException(e); }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Task> parseContent(Map<String, Object> response) {
        return (List<Task>) response.getOrDefault("content", List.of());
    }
}
