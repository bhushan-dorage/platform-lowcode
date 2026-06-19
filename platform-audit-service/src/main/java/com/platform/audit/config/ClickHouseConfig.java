package com.platform.audit.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class ClickHouseConfig {

    /**
     * DataSource wired to ClickHouse via the official ClickHouse JDBC driver.
     *
     * Spring Boot's DataSourceAutoConfiguration is excluded in application.yml so that
     * this manually-declared DataSource is the only one in the application context.
     */
    @Bean
    @Qualifier("clickhouse")
    public DataSource clickhouseDataSource(
            @Value("${clickhouse.url}") String url,
            @Value("${clickhouse.username:default}") String user,
            @Value("${clickhouse.password:}") String password) {

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        return ds;
    }

    /**
     * Primary JdbcTemplate backed by the ClickHouse DataSource.
     *
     * Marked @Primary so that Spring auto-wires this template into repositories
     * without requiring an explicit @Qualifier at every injection point.
     */
    @Bean
    @Primary
    public JdbcTemplate clickhouseJdbc(@Qualifier("clickhouse") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
