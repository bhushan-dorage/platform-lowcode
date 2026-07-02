package com.platform.webhook.config;

import com.platform.webhook.delivery.DeliverySleeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebhookConfig {

    @Bean
    public HttpClient webhookHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public DeliverySleeper webhookSleeper() {
        return Thread::sleep;
    }
}
