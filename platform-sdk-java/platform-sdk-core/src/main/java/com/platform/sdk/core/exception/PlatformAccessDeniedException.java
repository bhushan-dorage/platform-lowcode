package com.platform.sdk.core.exception;
public class PlatformAccessDeniedException extends PlatformSdkException {
    public PlatformAccessDeniedException(String message) { super(message, 403); }
}
