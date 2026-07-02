package com.platform.sdk.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformSdkConfig {
    @Builder.Default private String baseUrl = "http://localhost:8000";
    @Builder.Default private String tokenUrl = "http://localhost:8080/realms/platform/protocol/openid-connect/token";
    private String clientId;
    private String clientSecret;
    @Builder.Default private int maxRetries = 3;
    @Builder.Default private long connectTimeoutMs = 10_000;
    @Builder.Default private long readTimeoutMs = 30_000;
}
