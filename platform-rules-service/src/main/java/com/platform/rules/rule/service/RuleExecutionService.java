package com.platform.rules.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantContext;
import com.platform.rules.rule.dto.RuleExecutionRequest;
import com.platform.rules.rule.dto.RuleExecutionResponse;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecutionService {

    @Value("${kie.server.url:http://localhost:8180/kie-server/services/rest}")
    private String kieServerUrl;

    @Value("${kie.server.user:kieserver}")
    private String kieUser;

    @Value("${kie.server.password:kieserver1!}")
    private String kiePassword;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Timed(name = "rules.execution")
    public RuleExecutionResponse execute(RuleExecutionRequest req) {
        String tenantId = TenantContext.getTenantId();
        String containerId = req.containerId() != null ? req.containerId() : req.ruleSetKey();
        long start = System.currentTimeMillis();

        // KIE Server DMN evaluation endpoint
        String url = kieServerUrl + "/server/containers/" + containerId + "/dmn";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(kieUser, kiePassword);
        headers.set("X-Tenant-ID", tenantId);

        // Wrap inputs in KIE DMN request envelope
        Map<String, Object> body = Map.of(
                "model-namespace", "https://platform/" + tenantId + "/" + req.ruleSetKey(),
                "model-name", req.ruleSetKey(),
                "dmn-context", req.inputs()
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> dmnResult = result != null
                    ? (Map<String, Object>) result.getOrDefault("dmn-evaluation-result", Map.of())
                    : Map.of();

            long elapsed = System.currentTimeMillis() - start;
            log.info("Rule executed ruleSetKey={} tenantId={} durationMs={}", req.ruleSetKey(), tenantId, elapsed);
            return new RuleExecutionResponse(dmnResult, List.of(), elapsed, req.ruleSetKey(), tenantId);
        } catch (Exception ex) {
            log.error("KIE Server call failed ruleSetKey={} tenantId={}", req.ruleSetKey(), tenantId, ex);
            throw new RuntimeException("Rule execution failed: " + ex.getMessage(), ex);
        }
    }
}
