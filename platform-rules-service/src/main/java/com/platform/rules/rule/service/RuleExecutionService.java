package com.platform.rules.rule.service;

import com.platform.common.tenant.TenantContext;
import com.platform.rules.rule.dto.RuleExecutionRequest;
import com.platform.rules.rule.dto.RuleExecutionResponse;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.dmn.api.DmnDecisionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecutionService {

    private final DmnDecisionService dmnDecisionService;

    @Timed(value = "rules.execution")
    public RuleExecutionResponse execute(RuleExecutionRequest req) {
        String tenantId = TenantContext.getTenantId();
        long start = System.currentTimeMillis();

        try {
            // executeDecision() (not executeWithSingleResult()/executeDecisionWithSingleResult())
            // — a legitimately multi-row hit policy (COLLECT, RULE ORDER, OUTPUT ORDER) must not
            // turn into an exception here. execute() is the deprecated predecessor of this method.
            List<Map<String, Object>> rows = dmnDecisionService.createExecuteDecisionBuilder()
                    .decisionKey(req.ruleSetKey())
                    .tenantId(tenantId)
                    .variables(req.inputs())
                    .executeDecision();

            Map<String, Object> primary = rows.isEmpty() ? Map.of() : rows.get(0);
            long elapsed = System.currentTimeMillis() - start;
            log.info("Rule executed ruleSetKey={} tenantId={} rowCount={} durationMs={}",
                    req.ruleSetKey(), tenantId, rows.size(), elapsed);
            return new RuleExecutionResponse(primary, rows, List.of(), elapsed, req.ruleSetKey(), tenantId);
        } catch (Exception ex) {
            log.error("DMN decision execution failed ruleSetKey={} tenantId={}", req.ruleSetKey(), tenantId, ex);
            throw new RuntimeException("Rule execution failed: " + ex.getMessage(), ex);
        }
    }
}
