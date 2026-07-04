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

/** Publishes a Studio FORM artifact into platform-form-service's own persistence. */
@Slf4j
@Component
public class FormServiceClient {

    @Value("${runtime-services.form-service.base-url:http://localhost:8080}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final RuntimeServiceTokenManager tokenManager;

    public FormServiceClient(RestTemplate restTemplate, RuntimeServiceTokenManager tokenManager) {
        this.restTemplate = restTemplate;
        this.tokenManager = tokenManager;
    }

    public void publish(String formKey, String name, FormPublishPayload payload) {
        HttpHeaders headers = authHeaders();
        boolean exists;
        try {
            restTemplate.exchange(baseUrl + "/api/v1/forms/" + formKey, HttpMethod.GET,
                    new HttpEntity<>(headers), Void.class);
            exists = true;
        } catch (HttpClientErrorException.NotFound notFound) {
            exists = false;
        } catch (Exception ex) {
            throw new RuntimeServiceBridgeException("Failed to check form-service for formKey=" + formKey, ex);
        }

        try {
            if (exists) {
                restTemplate.exchange(baseUrl + "/api/v1/forms/" + formKey + "/versions", HttpMethod.POST,
                        new HttpEntity<>(Map.of(
                                "jsonSchema", payload.jsonSchema(),
                                "uiSchema", payload.uiSchema()
                        ), headers), Void.class);
            } else {
                restTemplate.exchange(baseUrl + "/api/v1/forms", HttpMethod.POST,
                        new HttpEntity<>(Map.of(
                                "formKey", formKey,
                                "name", name,
                                "jsonSchema", payload.jsonSchema(),
                                "uiSchema", payload.uiSchema()
                        ), headers), Void.class);
            }
            log.info("Published form to form-service formKey={} tenantId={}", formKey, TenantContext.getTenantId());
        } catch (Exception ex) {
            throw new RuntimeServiceBridgeException("Failed to publish form " + formKey + " to form-service", ex);
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
