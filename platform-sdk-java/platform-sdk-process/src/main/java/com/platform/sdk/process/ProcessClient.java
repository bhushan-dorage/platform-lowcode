package com.platform.sdk.process;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.sdk.core.http.PlatformHttpClient;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class ProcessClient {

    private final PlatformHttpClient http;

    public ProcessStartBuilder start(String processKey) {
        return new ProcessStartBuilder(http, processKey);
    }

    public ProcessQuery query() {
        return new ProcessQuery(http);
    }

    public ProcessInstance findById(String processId) throws IOException {
        return http.get("/v1/processes/" + processId, new TypeReference<>() {});
    }

    public ProcessInstance findByBusinessKey(String businessKey) throws IOException {
        return http.get("/v1/processes?businessKey=" + businessKey, new TypeReference<>() {});
    }

    public void signal(String processId, String signalName, Map<String, Object> variables) throws IOException {
        http.post("/v1/processes/" + processId + "/signal/" + signalName, variables, new TypeReference<Void>() {});
    }

    public void terminate(String processId, String reason) throws IOException {
        http.post("/v1/processes/" + processId + "/terminate",
                Map.of("reason", reason), new TypeReference<Void>() {});
    }
}
