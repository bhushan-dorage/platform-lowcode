package com.platform.sdk.task;

import com.platform.sdk.core.http.PlatformHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCompletionBuilderTest {

    @Mock
    private PlatformHttpClient http;

    @Test
    void submit_postsToCorrectUrl() throws IOException {
        new TaskCompletionBuilder(http, "task-42")
                .outcome("APPROVED")
                .variable("comment", "Looks good")
                .submit();

        verify(http).post(eq("/v1/tasks/task-42/complete"), argThat(body -> {
            Map<?, ?> map = (Map<?, ?>) body;
            return "APPROVED".equals(map.get("outcome"));
        }), any());
    }

    @Test
    void builder_chainingReturnsSelf() {
        TaskCompletionBuilder builder = new TaskCompletionBuilder(http, "t1");
        assert builder.outcome("x") == builder;
        assert builder.variable("k", "v") == builder;
        assert builder.formData(Map.of("a", 1)) == builder;
    }
}
