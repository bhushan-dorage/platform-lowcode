package com.platform.studio.artifact.bridge;

import com.platform.common.tenant.TenantContext;
import com.platform.studio.auth.RuntimeServiceTokenManager;
import com.platform.studio.exception.RuntimeServiceBridgeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Publishes a Studio DATA_MODEL artifact into platform-data-service's own persistence. */
@Slf4j
@Component
public class DataServiceClient {

    @Value("${runtime-services.data-service.base-url:http://localhost:8080}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final RuntimeServiceTokenManager tokenManager;

    public DataServiceClient(RestTemplate restTemplate, RuntimeServiceTokenManager tokenManager) {
        this.restTemplate = restTemplate;
        this.tokenManager = tokenManager;
    }

    public void publish(String entityType, String displayName, String jsonSchema) {
        HttpHeaders headers = authHeaders();
        try {
            restTemplate.exchange(baseUrl + "/api/v1/entities/definitions", HttpMethod.POST,
                    new HttpEntity<>(Map.of(
                            "entityType", entityType,
                            "displayName", displayName,
                            "schema", jsonSchema
                    ), headers), Void.class);
            log.info("Defined entity in data-service entityType={} tenantId={}", entityType, TenantContext.getTenantId());
        } catch (HttpClientErrorException.BadRequest alreadyExists) {
            updateExisting(entityType, displayName, jsonSchema, headers);
        } catch (Exception ex) {
            throw new RuntimeServiceBridgeException("Failed to define entity " + entityType + " in data-service", ex);
        }
    }

    private void updateExisting(String entityType, String displayName, String jsonSchema, HttpHeaders headers) {
        try {
            restTemplate.exchange(baseUrl + "/api/v1/entities/definitions/" + entityType, HttpMethod.PUT,
                    new HttpEntity<>(Map.of(
                            "displayName", displayName,
                            "schema", jsonSchema
                    ), headers), Void.class);
            log.info("Updated entity definition in data-service entityType={} tenantId={}", entityType, TenantContext.getTenantId());
        } catch (Exception ex) {
            throw new RuntimeServiceBridgeException("Failed to update entity definition " + entityType + " in data-service", ex);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenManager.getToken());
        headers.set("X-Tenant-ID", TenantContext.getTenantId());
        return headers;
    }
}
