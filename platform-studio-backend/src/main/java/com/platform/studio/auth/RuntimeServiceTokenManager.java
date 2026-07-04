package com.platform.studio.auth;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

/**
 * Mints and caches a client-credentials token for studio-backend's own service account, used to
 * authenticate outbound calls to platform-form-service / platform-data-service. Modeled on
 * platform-sdk-core's PlatformTokenManager, which does the same for external SDK consumers.
 */
@Slf4j
@Component
public class RuntimeServiceTokenManager {

    @Value("${keycloak.token-url:http://localhost:8080/realms/platform/protocol/openid-connect/token}")
    private String tokenUrl;

    @Value("${keycloak.client-id:platform-api}")
    private String clientId;

    @Value("${keycloak.client-secret:platform-api-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public RuntimeServiceTokenManager(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=client_credentials"
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret;

        try {
            JsonNode json = restTemplate.postForObject(tokenUrl, new HttpEntity<>(body, headers), JsonNode.class);
            if (json == null || !json.hasNonNull("access_token")) {
                throw new IllegalStateException("Token response missing access_token");
            }
            cachedToken = json.get("access_token").asText();
            long expiresIn = json.path("expires_in").asLong(300);
            expiresAt = Instant.now().plusSeconds(expiresIn);
            return cachedToken;
        } catch (Exception ex) {
            log.error("Failed to fetch runtime-service token", ex);
            throw new IllegalStateException("Failed to fetch runtime-service token", ex);
        }
    }
}
