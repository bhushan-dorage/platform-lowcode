package com.platform.sdk.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.sdk.core.PlatformSdkConfig;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;

public class PlatformTokenManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");

    private final OkHttpClient httpClient;
    private final PlatformSdkConfig config;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public PlatformTokenManager(OkHttpClient httpClient, PlatformSdkConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    public String getToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
                return cachedToken;
            }
            return fetchNewToken();
        }
    }

    private String fetchNewToken() {
        String body = "grant_type=client_credentials"
                + "&client_id=" + config.getClientId()
                + "&client_secret=" + config.getClientSecret();
        Request request = new Request.Builder()
                .url(config.getTokenUrl())
                .post(RequestBody.create(body, FORM))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Token fetch failed: HTTP " + response.code());
            }
            JsonNode json = MAPPER.readTree(response.body().string());
            cachedToken = json.get("access_token").asText();
            long expiresIn = json.path("expires_in").asLong(300);
            expiresAt = Instant.now().plusSeconds(expiresIn);
            return cachedToken;
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch OAuth2 token", e);
        }
    }
}
