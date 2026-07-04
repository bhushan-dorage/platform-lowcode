package com.platform.rules.rule.service;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.rules.rule.dto.RuleExecutionRequest;
import com.platform.rules.rule.dto.RuleExecutionResponse;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleExecutionServiceTest {

    @Mock DmnDecisionService dmnDecisionService;
    @InjectMocks RuleExecutionService ruleExecutionService;

    @BeforeEach
    void setup() {
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void execute_singleRowResult_setsOutputsToFirstRow() {
        ExecuteDecisionBuilder builder = mock(ExecuteDecisionBuilder.class, RETURNS_SELF);
        when(dmnDecisionService.createExecuteDecisionBuilder()).thenReturn(builder);
        Map<String, Object> row = Map.of("approved", true);
        when(builder.executeDecision()).thenReturn(List.of(row));

        RuleExecutionResponse response = ruleExecutionService.execute(
                new RuleExecutionRequest("loan-eligibility", Map.of("income", 50000), null));

        verify(builder).decisionKey("loan-eligibility");
        verify(builder).tenantId("acme");
        verify(builder).variables(Map.of("income", 50000));
        assertThat(response.outputs()).isEqualTo(row);
        assertThat(response.outputRows()).containsExactly(row);
        assertThat(response.ruleSetKey()).isEqualTo("loan-eligibility");
        assertThat(response.tenantId()).isEqualTo("acme");
    }

    @Test
    void execute_multiRowHitPolicy_preservesAllRowsWithoutThrowing() {
        ExecuteDecisionBuilder builder = mock(ExecuteDecisionBuilder.class, RETURNS_SELF);
        when(dmnDecisionService.createExecuteDecisionBuilder()).thenReturn(builder);
        Map<String, Object> row1 = Map.of("discount", 10);
        Map<String, Object> row2 = Map.of("discount", 20);
        when(builder.executeDecision()).thenReturn(List.of(row1, row2));

        RuleExecutionResponse response = ruleExecutionService.execute(
                new RuleExecutionRequest("discount-rules", Map.of(), null));

        assertThat(response.outputs()).isEqualTo(row1);
        assertThat(response.outputRows()).containsExactly(row1, row2);
    }

    @Test
    void execute_emptyResult_returnsEmptyMapAndEmptyList() {
        ExecuteDecisionBuilder builder = mock(ExecuteDecisionBuilder.class, RETURNS_SELF);
        when(dmnDecisionService.createExecuteDecisionBuilder()).thenReturn(builder);
        when(builder.executeDecision()).thenReturn(List.of());

        RuleExecutionResponse response = ruleExecutionService.execute(
                new RuleExecutionRequest("no-match", Map.of(), null));

        assertThat(response.outputs()).isEmpty();
        assertThat(response.outputRows()).isEmpty();
    }

    @Test
    void execute_engineThrows_wrapsInRuntimeException() {
        ExecuteDecisionBuilder builder = mock(ExecuteDecisionBuilder.class, RETURNS_SELF);
        when(dmnDecisionService.createExecuteDecisionBuilder()).thenReturn(builder);
        when(builder.executeDecision()).thenThrow(new RuntimeException("decision not found"));

        assertThatThrownBy(() -> ruleExecutionService.execute(
                new RuleExecutionRequest("missing-decision", Map.of(), null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rule execution failed");
    }
}
