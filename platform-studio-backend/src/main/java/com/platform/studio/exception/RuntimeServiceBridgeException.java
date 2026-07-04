package com.platform.studio.exception;

/** Thrown when publishing an artifact to its runtime service (form-service, data-service) fails. */
public class RuntimeServiceBridgeException extends RuntimeException {
    public RuntimeServiceBridgeException(String msg, Throwable cause) { super(msg, cause); }
}
