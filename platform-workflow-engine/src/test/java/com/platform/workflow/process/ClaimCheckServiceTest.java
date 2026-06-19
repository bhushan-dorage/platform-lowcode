package com.platform.workflow.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimCheckServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private ClaimCheckService claimCheckService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(claimCheckService, "thresholdBytes", 10240);
        ReflectionTestUtils.setField(claimCheckService, "objectMapper", objectMapper);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        TenantContext.set("acme", TenantTier.ENTERPRISE);
    }

    @AfterEach
    void teardown() {
        TenantContext.clear();
    }

    @Test
    void store_returnsNullForSmallValues() {
        String refId = claimCheckService.store("acme", "name", "small value");

        assertThat(refId).isNull();
        verify(valueOps, never()).set(anyString(), anyString(), any());
    }

    @Test
    void store_storesInRedisForLargeValues() {
        String largeValue = "x".repeat(20000);

        String refId = claimCheckService.store("acme", "bigVar", largeValue);

        assertThat(refId).isNotNull();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), anyString(), any());
        assertThat(keyCaptor.getValue()).startsWith("acme:claimcheck:");
    }

    @Test
    void store_usesTenantScopedKey() {
        String largeValue = "x".repeat(20000);

        claimCheckService.store("tenant-a", "var", largeValue);
        claimCheckService.store("tenant-b", "var", largeValue);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps, times(2)).set(keyCaptor.capture(), anyString(), any());
        assertThat(keyCaptor.getAllValues().get(0)).startsWith("tenant-a:");
        assertThat(keyCaptor.getAllValues().get(1)).startsWith("tenant-b:");
    }

    @Test
    void resolveVariables_replacesRefEntriesWithPayloads() throws Exception {
        String payload = objectMapper.writeValueAsString("resolved-payload");
        when(valueOps.get("acme:claimcheck:ref-123")).thenReturn(payload);

        Map<String, Object> raw = Map.of(
                "normalKey", "normalValue",
                "bigData__ref", "ref-123"
        );
        Map<String, Object> resolved = claimCheckService.resolveVariables("acme", raw);

        assertThat(resolved).containsKey("normalKey").containsKey("bigData");
        assertThat(resolved).doesNotContainKey("bigData__ref");
        assertThat(resolved.get("normalKey")).isEqualTo("normalValue");
    }

    @Test
    void prepareVariables_convertsLargeValuesToRefs() {
        String largeValue = "x".repeat(20000);
        Map<String, Object> vars = Map.of("smallVar", "small", "bigVar", largeValue);

        Map<String, Object> prepared = claimCheckService.prepareVariables("acme", vars);

        assertThat(prepared).containsKey("smallVar");
        assertThat(prepared).containsKey("bigVar__ref");
        assertThat(prepared).doesNotContainKey("bigVar");
        assertThat(prepared.get("smallVar")).isEqualTo("small");
    }
}
