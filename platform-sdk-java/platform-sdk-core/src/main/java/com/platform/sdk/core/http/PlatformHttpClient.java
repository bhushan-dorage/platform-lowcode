package com.platform.sdk.core.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.sdk.core.PlatformSdkConfig;
import com.platform.sdk.core.auth.PlatformTokenManager;
import com.platform.sdk.core.exception.*;
import okhttp3.*;

import java.io.IOException;

public class PlatformHttpClient {

    private static final MediaType JSON = MediaType.get("application/json");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final OkHttpClient httpClient;
    private final PlatformTokenManager tokenManager;
    private final SdkRetryExecutor retryExecutor;
    private final String baseUrl;

    public PlatformHttpClient(PlatformSdkConfig config) {
        this.baseUrl = config.getBaseUrl();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofMillis(config.getConnectTimeoutMs()))
                .readTimeout(java.time.Duration.ofMillis(config.getReadTimeoutMs()))
                .build();
        this.tokenManager = new PlatformTokenManager(httpClient, config);
        this.retryExecutor = new SdkRetryExecutor(config.getMaxRetries());
    }

    public <T> T get(String path, TypeReference<T> type) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer " + tokenManager.getToken())
                .get()
                .build();
        return execute(request, type);
    }

    public <T> T post(String path, Object body, TypeReference<T> type) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer " + tokenManager.getToken())
                .post(RequestBody.create(json, JSON))
                .build();
        return execute(request, type);
    }

    public <T> T put(String path, Object body, TypeReference<T> type) throws IOException {
        String json = MAPPER.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer " + tokenManager.getToken())
                .put(RequestBody.create(json, JSON))
                .build();
        return execute(request, type);
    }

    public void delete(String path) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer " + tokenManager.getToken())
                .delete()
                .build();
        execute(request, new TypeReference<Void>() {});
    }

    private <T> T execute(Request request, TypeReference<T> type) throws IOException {
        try (Response response = retryExecutor.execute(() -> {
            try {
                return httpClient.newCall(request).execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throwForStatus(response.code(), responseBody);
            }
            if (type.getType() == Void.class || responseBody.isBlank()) {
                return null;
            }
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode data = root.has("data") ? root.get("data") : root;
            return MAPPER.treeToValue(data, MAPPER.constructType(type.getType()));
        }
    }

    private void throwForStatus(int code, String body) {
        String message = extractMessage(body);
        switch (code) {
            case 400 -> throw new ValidationException(message);
            case 403 -> throw new PlatformAccessDeniedException(message);
            case 404 -> throw new EntityNotFoundException(message);
            case 409 -> throw new TaskAlreadyClaimedException(message);
            case 429 -> throw new RateLimitException(message, 60);
            default -> throw new PlatformSdkException("HTTP " + code + ": " + message, code);
        }
    }

    private String extractMessage(String body) {
        try {
            JsonNode node = MAPPER.readTree(body);
            if (node.has("message")) return node.get("message").asText();
            if (node.has("error")) return node.get("error").asText();
        } catch (Exception ignored) {}
        return body.isEmpty() ? "Unknown error" : body;
    }
}
