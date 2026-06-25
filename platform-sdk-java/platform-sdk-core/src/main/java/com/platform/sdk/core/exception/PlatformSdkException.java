package com.platform.sdk.core.exception;

public class PlatformSdkException extends RuntimeException {
    private final int statusCode;
    public PlatformSdkException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    public PlatformSdkException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }
    public int getStatusCode() { return statusCode; }
}
