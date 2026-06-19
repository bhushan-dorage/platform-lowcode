package com.platform.entitlements.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache effectivePermissions = new CaffeineCache("effective-permissions",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(10_000)
                        .recordStats()
                        .build());

        CaffeineCache fieldPolicies = new CaffeineCache("field-policies",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(5_000)
                        .recordStats()
                        .build());

        CaffeineCache dataPolicies = new CaffeineCache("data-policies",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(5_000)
                        .recordStats()
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(effectivePermissions, fieldPolicies, dataPolicies));
        return manager;
    }
}
