package com.platform.sdk.core.http;

import com.platform.sdk.core.exception.RateLimitException;
import okhttp3.Response;

import java.io.IOException;
import java.util.function.Supplier;

public class SdkRetryExecutor {

    private final int maxRetries;

    public SdkRetryExecutor(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Response execute(Supplier<Response> call) throws IOException {
        IOException lastNetworkError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                long backoffMs = (long) Math.pow(2, attempt - 1) * 1000L;
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry backoff", e);
                }
            }
            try {
                Response response = call.get();
                int code = response.code();
                if (code == 429) {
                    String retryAfter = response.header("Retry-After", "60");
                    response.close();
                    throw new RateLimitException("Rate limited by server", Long.parseLong(retryAfter));
                }
                if (code >= 500 && attempt < maxRetries) {
                    response.close();
                    continue;
                }
                return response;
            } catch (RateLimitException e) {
                throw e;
            } catch (IOException e) {
                lastNetworkError = e;
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException cause) {
                    lastNetworkError = cause;
                } else {
                    throw e;
                }
            }
        }
        throw lastNetworkError != null ? lastNetworkError : new IOException("All retries exhausted");
    }
}
