package com.platform.studio.artifact.bridge;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.studio.auth.RuntimeServiceTokenManager;
import com.platform.studio.exception.RuntimeServiceBridgeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class FormServiceClientTest {

    @Mock RuntimeServiceTokenManager tokenManager;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private FormServiceClient client;

    private static final String BASE_URL = "http://form-service";

    @BeforeEach
    void setup() {
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new FormServiceClient(restTemplate, tokenManager);
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void publish_newForm_createsIt() {
        when(tokenManager.getToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/api/v1/forms/intake-form"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(BASE_URL + "/api/v1/forms"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-abc"))
                .andExpect(header("X-Tenant-ID", "acme"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        client.publish("intake-form", "Intake Form", new FormPublishPayload("{}", "[]"));

        server.verify();
    }

    @Test
    void publish_existingForm_publishesNewVersion() {
        when(tokenManager.getToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/api/v1/forms/intake-form"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/forms/intake-form/versions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        client.publish("intake-form", "Intake Form", new FormPublishPayload("{}", "[]"));

        server.verify();
    }

    @Test
    void publish_downstreamFailure_wrapsInBridgeException() {
        when(tokenManager.getToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/api/v1/forms/intake-form"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.publish("intake-form", "Intake Form", new FormPublishPayload("{}", "[]")))
                .isInstanceOf(RuntimeServiceBridgeException.class);
    }
}
