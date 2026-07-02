package com.platform.sdk.core.exception;
public class RateLimitException extends PlatformSdkException {
    private final long retryAfterSeconds;
    public RateLimitException(String message, long retryAfterSeconds) {
        super(message, 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
