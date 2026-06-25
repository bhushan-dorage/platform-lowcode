package com.platform.sdk.core.exception;
public class EntityNotFoundException extends PlatformSdkException {
    public EntityNotFoundException(String message) { super(message, 404); }
}
