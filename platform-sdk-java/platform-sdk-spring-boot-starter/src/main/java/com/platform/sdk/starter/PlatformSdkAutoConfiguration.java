package com.platform.sdk.starter;

import com.platform.sdk.core.PlatformSdkConfig;
import com.platform.sdk.core.http.PlatformHttpClient;
import com.platform.sdk.process.ProcessClient;
import com.platform.sdk.task.TaskClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PlatformSdkProperties.class)
public class PlatformSdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PlatformHttpClient platformHttpClient(PlatformSdkProperties props) {
        PlatformSdkConfig config = PlatformSdkConfig.builder()
                .baseUrl(props.getBaseUrl())
                .tokenUrl(props.getTokenUrl())
                .clientId(props.getClientId())
                .clientSecret(props.getClientSecret())
                .maxRetries(props.getMaxRetries())
                .connectTimeoutMs(props.getConnectTimeoutMs())
                .readTimeoutMs(props.getReadTimeoutMs())
                .build();
        return new PlatformHttpClient(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessClient processClient(PlatformHttpClient httpClient) {
        return new ProcessClient(httpClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskClient taskClient(PlatformHttpClient httpClient) {
        return new TaskClient(httpClient);
    }
}
