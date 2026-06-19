package com.platform.entitlements.enforcement;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String msg) { super(msg); }
}
