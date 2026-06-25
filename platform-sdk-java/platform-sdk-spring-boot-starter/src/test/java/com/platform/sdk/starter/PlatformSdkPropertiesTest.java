package com.platform.sdk.starter;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformSdkPropertiesTest {

    @Test
    void defaults_areCorrect() {
        PlatformSdkProperties props = new PlatformSdkProperties();
        assertThat(props.getBaseUrl()).isEqualTo("http://localhost:8000");
        assertThat(props.getMaxRetries()).isEqualTo(3);
        assertThat(props.getConnectTimeoutMs()).isEqualTo(10_000);
        assertThat(props.getReadTimeoutMs()).isEqualTo(30_000);
    }

    @Test
    void setters_work() {
        PlatformSdkProperties props = new PlatformSdkProperties();
        props.setClientId("my-client");
        props.setClientSecret("my-secret");
        props.setBaseUrl("http://platform.example.com");
        assertThat(props.getClientId()).isEqualTo("my-client");
        assertThat(props.getBaseUrl()).isEqualTo("http://platform.example.com");
    }
}
