package com.platform.sdk.process;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.sdk.core.http.PlatformHttpClient;
import com.platform.sdk.core.pagination.PagedResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessQuery {

    private final PlatformHttpClient http;
    private String processKey;
    private String status;
    private String businessKey;
    private int pageSize = 20;

    ProcessQuery(PlatformHttpClient http) {
        this.http = http;
    }

    public ProcessQuery processKey(String processKey) {
        this.processKey = processKey;
        return this;
    }

    public ProcessQuery status(String status) {
        this.status = status;
        return this;
    }

    public ProcessQuery businessKey(String businessKey) {
        this.businessKey = businessKey;
        return this;
    }

    public ProcessQuery pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PagedResult<ProcessInstance> fetch() throws IOException {
        return fetchPage(null);
    }

    private PagedResult<ProcessInstance> fetchPage(String cursor) throws IOException {
        StringBuilder url = new StringBuilder("/v1/processes?pageSize=").append(pageSize);
        if (processKey != null) url.append("&processKey=").append(processKey);
        if (status != null) url.append("&status=").append(status);
        if (businessKey != null) url.append("&businessKey=").append(businessKey);
        if (cursor != null) url.append("&cursor=").append(cursor);

        Map<String, Object> response = http.get(url.toString(), new TypeReference<>() {});
        List<ProcessInstance> content = parseContent(response);
        String nextCursor = (String) response.get("cursor");
        boolean hasMore = Boolean.TRUE.equals(response.get("hasMore"));
        return new PagedResult<>(content, nextCursor, hasMore, c -> {
            try { return fetchPage(c); } catch (IOException e) { throw new RuntimeException(e); }
        });
    }

    @SuppressWarnings("unchecked")
    private List<ProcessInstance> parseContent(Map<String, Object> response) {
        return (List<ProcessInstance>) response.getOrDefault("content", List.of());
    }
}
