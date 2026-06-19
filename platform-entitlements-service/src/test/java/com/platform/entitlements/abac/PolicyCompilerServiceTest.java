package com.platform.entitlements.abac;

import com.platform.entitlements.abac.domain.DataPolicy;
import com.platform.entitlements.abac.domain.PolicyOperation;
import com.platform.entitlements.abac.repository.DataPolicyRepository;
import com.platform.entitlements.abac.service.PolicyCompilerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyCompilerServiceTest {

    @Mock DataPolicyRepository policyRepo;

    @InjectMocks PolicyCompilerService compilerService;

    private DataPolicy makePolicy(String yaml) {
        DataPolicy p = new DataPolicy();
        p.setTenantId("acme");
        p.setRoleName("analyst");
        p.setEntityType("Invoice");
        p.setOperation(PolicyOperation.READ);
        p.setPolicyYaml(yaml);
        return p;
    }

    @Test
    void compile_equalsCondition() {
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        String yaml = """
                conditions:
                  - field: status
                    op: EQUALS
                    value: APPROVED
                """;
        DataPolicy policy = makePolicy(yaml);
        String predicate = compilerService.compile(policy);
        assertThat(predicate).isEqualTo("status = 'APPROVED'");
    }

    @Test
    void compile_actorAttributeReference() {
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        String yaml = """
                conditions:
                  - field: branchCode
                    op: EQUALS
                    value: ${actor.attributes.branchId}
                """;
        DataPolicy policy = makePolicy(yaml);
        String predicate = compilerService.compile(policy);
        assertThat(predicate).isEqualTo("branch_code = :actorBranchId");
    }

    @Test
    void compile_inCondition() {
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        String yaml = """
                conditions:
                  - field: region
                    op: IN
                    values:
                      - APAC
                      - EMEA
                """;
        DataPolicy policy = makePolicy(yaml);
        String predicate = compilerService.compile(policy);
        assertThat(predicate).isEqualTo("region IN ('APAC', 'EMEA')");
    }

    @Test
    void compile_multipleConditionsJoinedWithAnd() {
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        String yaml = """
                conditions:
                  - field: status
                    op: EQUALS
                    value: ACTIVE
                  - field: amount
                    op: LESS_THAN
                    value: '10000'
                """;
        DataPolicy policy = makePolicy(yaml);
        String predicate = compilerService.compile(policy);
        assertThat(predicate).isEqualTo("status = 'ACTIVE' AND amount < '10000'");
    }

    @Test
    void compile_unknownOperatorThrows() {
        String yaml = """
                conditions:
                  - field: name
                    op: FUZZY
                    value: test
                """;
        DataPolicy policy = makePolicy(yaml);
        assertThatThrownBy(() -> compilerService.compile(policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to compile policy");
    }
}
