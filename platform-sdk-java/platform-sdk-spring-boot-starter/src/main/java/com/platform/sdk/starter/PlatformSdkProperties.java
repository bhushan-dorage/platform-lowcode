package com.platform.sdk.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "platform.sdk")
public class PlatformSdkProperties {
    private String baseUrl = "http://localhost:8000";
    private String tokenUrl = "http://localhost:8080/realms/platform/protocol/openid-connect/token";
    private String clientId;
    private String clientSecret;
    private int maxRetries = 3;
    private long connectTimeoutMs = 10_000;
    private long readTimeoutMs = 30_000;
}
