package com.platform.sdk.core.http;

import com.platform.sdk.core.exception.RateLimitException;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class SdkRetryExecutorTest {

    private static Response response(int code) {
        return new Response.Builder()
                .request(new Request.Builder().url("http://localhost/test").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .build();
    }

    @Test
    void execute_successOnFirstAttempt() throws IOException {
        SdkRetryExecutor executor = new SdkRetryExecutor(3);
        Response res = executor.execute(() -> response(200));
        assertThat(res.code()).isEqualTo(200);
    }

    @Test
    void execute_retries5xxAndSucceeds() throws IOException {
        SdkRetryExecutor executor = new SdkRetryExecutor(3);
        AtomicInteger count = new AtomicInteger(0);
        Response res = executor.execute(() -> {
            if (count.incrementAndGet() < 3) return response(500);
            return response(200);
        });
        assertThat(res.code()).isEqualTo(200);
        assertThat(count.get()).isEqualTo(3);
    }

    @Test
    void execute_throws429AsRateLimitException() {
        SdkRetryExecutor executor = new SdkRetryExecutor(3);
        assertThatThrownBy(() -> executor.execute(() -> {
            Response r = new Response.Builder()
                    .request(new Request.Builder().url("http://localhost/test").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("")
                    .header("Retry-After", "30")
                    .build();
            return r;
        })).isInstanceOf(RateLimitException.class)
                .satisfies(e -> assertThat(((RateLimitException) e).getRetryAfterSeconds()).isEqualTo(30));
    }

    @Test
    void execute_networkErrorExhaustsRetries() {
        SdkRetryExecutor executor = new SdkRetryExecutor(2);
        assertThatThrownBy(() -> executor.execute(() -> {
            throw new RuntimeException(new IOException("connection refused"));
        })).isInstanceOf(IOException.class);
    }
}
