package com.platform.entitlements.field;

import com.platform.entitlements.field.domain.FieldAccessLevel;
import com.platform.entitlements.field.domain.FieldPolicy;
import com.platform.entitlements.field.domain.MaskPattern;
import com.platform.entitlements.field.repository.FieldPolicyRepository;
import com.platform.entitlements.field.service.FieldMaskingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldMaskingServiceTest {

    @Mock FieldPolicyRepository fieldPolicyRepo;
    @InjectMocks FieldMaskingService service;

    private FieldPolicy policy(String field, FieldAccessLevel level, MaskPattern mask) {
        FieldPolicy p = new FieldPolicy();
        p.setId(UUID.randomUUID());
        p.setTenantId("acme");
        p.setRoleName("analyst");
        p.setEntityType("Invoice");
        p.setFieldName(field);
        p.setAccessLevel(level);
        p.setMaskPattern(mask);
        return p;
    }

    @Test
    void deny_removesField() {
        when(fieldPolicyRepo.findByTenantIdAndRoleNameAndEntityType("acme", "analyst", "Invoice"))
                .thenReturn(List.of(policy("secretField", FieldAccessLevel.DENY, null)));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "123");
        data.put("secretField", "sensitive");

        Map<String, Object> result = service.applyFieldPolicies("acme", "analyst", "Invoice", data);
        assertThat(result).containsKey("id").doesNotContainKey("secretField");
    }

    @Test
    void masked_pan_appliesMaskPattern() {
        when(fieldPolicyRepo.findByTenantIdAndRoleNameAndEntityType("acme", "analyst", "Invoice"))
                .thenReturn(List.of(policy("cardNumber", FieldAccessLevel.MASKED, MaskPattern.PAN)));

        Map<String, Object> data = Map.of("cardNumber", "1234567890123456");
        Map<String, Object> result = service.applyFieldPolicies("acme", "analyst", "Invoice", data);
        assertThat(result.get("cardNumber")).isEqualTo("1234****3456");
    }

    @Test
    void masked_email_appliesMaskPattern() {
        when(fieldPolicyRepo.findByTenantIdAndRoleNameAndEntityType("acme", "analyst", "Invoice"))
                .thenReturn(List.of(policy("email", FieldAccessLevel.MASKED, MaskPattern.EMAIL)));

        Map<String, Object> data = Map.of("email", "bob@example.com");
        Map<String, Object> result = service.applyFieldPolicies("acme", "analyst", "Invoice", data);
        assertThat(result.get("email")).isEqualTo("b*****@example.com");
    }

    @Test
    void masked_mobile_appliesMaskPattern() {
        when(fieldPolicyRepo.findByTenantIdAndRoleNameAndEntityType("acme", "analyst", "Invoice"))
                .thenReturn(List.of(policy("phone", FieldAccessLevel.MASKED, MaskPattern.MOBILE)));

        Map<String, Object> data = Map.of("phone", "0412345678");
        Map<String, Object> result = service.applyFieldPolicies("acme", "analyst", "Invoice", data);
        assertThat(result.get("phone")).isEqualTo("****5678");
    }

    @Test
    void noPolicies_passesDataThrough() {
        when(fieldPolicyRepo.findByTenantIdAndRoleNameAndEntityType("acme", "analyst", "Invoice"))
                .thenReturn(List.of());

        Map<String, Object> data = Map.of("id", "1", "amount", 100);
        Map<String, Object> result = service.applyFieldPolicies("acme", "analyst", "Invoice", data);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void allow_includesField() {
        when(fieldPolicyRepo.findByTenantIdAndRoleNameAndEntityType("acme", "analyst", "Invoice"))
                .thenReturn(List.of(policy("amount", FieldAccessLevel.ALLOW, null)));

        Map<String, Object> data = Map.of("amount", 9999);
        Map<String, Object> result = service.applyFieldPolicies("acme", "analyst", "Invoice", data);
        assertThat(result.get("amount")).isEqualTo(9999);
    }
}
