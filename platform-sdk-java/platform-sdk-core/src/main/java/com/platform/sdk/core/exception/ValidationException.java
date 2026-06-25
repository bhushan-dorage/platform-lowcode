package com.platform.sdk.core.exception;
public class ValidationException extends PlatformSdkException {
    public ValidationException(String message) { super(message, 400); }
}
