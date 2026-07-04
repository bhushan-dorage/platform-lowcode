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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class DataServiceClientTest {

    @Mock RuntimeServiceTokenManager tokenManager;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private DataServiceClient client;

    private static final String BASE_URL = "http://data-service";

    @BeforeEach
    void setup() {
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new DataServiceClient(restTemplate, tokenManager);
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void publish_newEntity_createsIt() {
        when(tokenManager.getToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/api/v1/entities/definitions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        client.publish("invoice", "Invoice", "{\"type\":\"object\"}");

        server.verify();
    }

    @Test
    void publish_existingEntity_fallsBackToUpdate() {
        when(tokenManager.getToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/api/v1/entities/definitions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        server.expect(requestTo(BASE_URL + "/api/v1/entities/definitions/invoice"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.publish("invoice", "Invoice", "{\"type\":\"object\"}");

        server.verify();
    }

    @Test
    void publish_downstreamServerError_wrapsInBridgeException() {
        when(tokenManager.getToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/api/v1/entities/definitions"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.publish("invoice", "Invoice", "{\"type\":\"object\"}"))
                .isInstanceOf(RuntimeServiceBridgeException.class);
    }
}
