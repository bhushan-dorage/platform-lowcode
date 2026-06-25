package com.platform.sdk.core.exception;
public class TaskAlreadyClaimedException extends PlatformSdkException {
    public TaskAlreadyClaimedException(String message) { super(message, 409); }
}
