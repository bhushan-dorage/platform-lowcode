package com.platform.common.config;

import com.platform.common.tenant.TenantRoutingDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Wires the TenantRoutingDataSource as the primary DataSource so that JPA, JdbcTemplate,
 * and TransactionManager all route through tenant-aware schema selection.
 *
 * The "platform_meta" key points to the default connection pool (the one configured via
 * spring.datasource.*) and is used for cross-tenant administrative operations and startup.
 * Additional tenant datasources can be registered at runtime by adding entries to the
 * routing map via {@link TenantRoutingDataSource#setTargetDataSources(Map)}.
 */
@Configuration
public class TenantDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource defaultDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public TenantRoutingDataSource routingDataSource(DataSource defaultDataSource) {
        TenantRoutingDataSource routingDs = new TenantRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(TenantRoutingDataSource.PLATFORM_META_KEY, defaultDataSource);
        // STARTER tenants share a schema on the same physical database; shared_starter
        // also maps to the default datasource for now — clusters can override at runtime.
        targetDataSources.put("shared_starter", defaultDataSource);

        routingDs.setTargetDataSources(targetDataSources);
        routingDs.setDefaultTargetDataSource(defaultDataSource);
        return routingDs;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(TenantRoutingDataSource routingDataSource) {
        return new JdbcTemplate(routingDataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(TenantRoutingDataSource routingDataSource) {
        return new DataSourceTransactionManager(routingDataSource);
    }
}
