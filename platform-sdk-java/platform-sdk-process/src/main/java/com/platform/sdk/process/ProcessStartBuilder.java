package com.platform.sdk.process;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.sdk.core.http.PlatformHttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProcessStartBuilder {

    private final PlatformHttpClient http;
    private final String processKey;
    private String businessKey;
    private String idempotencyKey;
    private String callbackUrl;
    private final Map<String, Object> variables = new HashMap<>();

    ProcessStartBuilder(PlatformHttpClient http, String processKey) {
        this.http = http;
        this.processKey = processKey;
    }

    public ProcessStartBuilder businessKey(String businessKey) {
        this.businessKey = businessKey;
        return this;
    }

    public ProcessStartBuilder idempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public ProcessStartBuilder variable(String name, Object value) {
        this.variables.put(name, value);
        return this;
    }

    public ProcessStartBuilder variables(Map<String, Object> vars) {
        this.variables.putAll(vars);
        return this;
    }

    public ProcessStartBuilder callbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
        return this;
    }

    public ProcessTracker submit() throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("processKey", processKey);
        if (businessKey != null) body.put("businessKey", businessKey);
        if (idempotencyKey != null) body.put("idempotencyKey", idempotencyKey);
        if (callbackUrl != null) body.put("callbackUrl", callbackUrl);
        if (!variables.isEmpty()) body.put("variables", variables);
        return http.post("/v1/processes", body, new TypeReference<>() {});
    }
}
