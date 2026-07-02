package com.platform.sdk.process;

import lombok.Data;

@Data
public class ProcessTracker {
    private String trackingId;
    private String statusUrl;
    private String status;
}
