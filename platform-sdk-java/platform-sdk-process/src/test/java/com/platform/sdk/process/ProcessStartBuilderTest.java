package com.platform.sdk.process;

import com.platform.sdk.core.http.PlatformHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessStartBuilderTest {

    @Mock
    private PlatformHttpClient http;

    @Test
    void submit_callsPostWithCorrectPayload() throws IOException {
        ProcessTracker tracker = new ProcessTracker();
        tracker.setTrackingId("t-1");
        when(http.post(eq("/v1/processes"), anyMap(), any())).thenReturn(tracker);

        ProcessTracker result = new ProcessStartBuilder(http, "order-process")
                .businessKey("order-123")
                .variable("amount", 99.99)
                .submit();

        assertThat(result.getTrackingId()).isEqualTo("t-1");
        verify(http).post(eq("/v1/processes"), argThat(body -> {
            Map<?, ?> map = (Map<?, ?>) body;
            return "order-process".equals(map.get("processKey"))
                    && "order-123".equals(map.get("businessKey"));
        }), any());
    }

    @Test
    void builder_chainingReturnsSelf() {
        ProcessStartBuilder builder = new ProcessStartBuilder(http, "key");
        assertThat(builder.businessKey("bk")).isSameAs(builder);
        assertThat(builder.idempotencyKey("ik")).isSameAs(builder);
        assertThat(builder.variable("k", "v")).isSameAs(builder);
        assertThat(builder.variables(Map.of("a", 1))).isSameAs(builder);
        assertThat(builder.callbackUrl("http://cb")).isSameAs(builder);
    }
}
