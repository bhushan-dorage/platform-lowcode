package com.platform.data.entity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantTier;
import com.platform.data.entity.ddl.EntityTableDdlService;
import com.platform.data.entity.ddl.FieldSchema;
import com.platform.data.entity.repository.EntityRecordDao;
import com.platform.data.exception.InvalidIdentifierException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the real-per-entity-table design against a real Postgres: schema-qualified DDL,
 * dynamic column types, additive ALTER, and the STARTER-tier physical-table-naming collision
 * fix. Uses the DDL/DAO classes directly (bypassing the full Spring Boot context) to keep this
 * test focused on the SQL-level question, matching the same approach used for the rules-service
 * DMN engine integration test.
 */
@Testcontainers
class EntityTableDdlIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static EntityTableDdlService ddlService;
    static EntityRecordDao recordDao;
    static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void setUp() throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        jdbcTemplate = new JdbcTemplate(dataSource);
        ddlService = new EntityTableDdlService(jdbcTemplate);
        recordDao = new EntityRecordDao(jdbcTemplate, new ObjectMapper());

        createSchema(dataSource, "acme_platform");
        createSchema(dataSource, "shared_starter");
    }

    private static void createSchema(DataSource dataSource, String schema) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        }
    }

    @Test
    void createTable_thenInsertGetUpdateAndPage_roundTripsCorrectly() {
        List<FieldSchema> fields = List.of(
                new FieldSchema("amount", "number", true),
                new FieldSchema("currency", "string", false));
        ddlService.createTable("acme_platform", "invoice", fields);

        UUID id = recordDao.insert("acme_platform", "invoice", fields, "acme",
                Map.of("amount", 100, "currency", "USD"), "alice");

        Optional<Map<String, Object>> found = recordDao.findById("acme_platform", "invoice", "acme", id);
        assertThat(found).isPresent();
        assertThat(found.get().get("amount")).isEqualTo(new java.math.BigDecimal("100"));
        assertThat(found.get().get("currency")).isEqualTo("USD");

        int updated = recordDao.update("acme_platform", "invoice", fields, "acme", id, Map.of("amount", 200));
        assertThat(updated).isEqualTo(1);
        assertThat(recordDao.findById("acme_platform", "invoice", "acme", id).get().get("amount"))
                .isEqualTo(new java.math.BigDecimal("200"));

        List<Map<String, Object>> page = recordDao.findPage("acme_platform", "invoice", "acme", null, 20);
        assertThat(page).hasSize(1);
    }

    @Test
    void applyAdditiveAlter_addsNewColumnToExistingTable() {
        List<FieldSchema> initialFields = List.of(new FieldSchema("name", "string", true));
        ddlService.createTable("acme_platform", "product", initialFields);

        ddlService.applyAdditiveAlter("acme_platform", "product",
                List.of(new FieldSchema("price", "number", false)));

        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'acme_platform' "
                        + "AND table_name = 'product' AND column_name = 'price'", Integer.class);
        assertThat(columnCount).isEqualTo(1);
    }

    @Test
    void starterTierTenants_withSameEntityType_getDistinctPhysicalTables() {
        String table1 = ddlService.physicalTableName("tenant1", TenantTier.STARTER, "invoice");
        String table2 = ddlService.physicalTableName("tenant2", TenantTier.STARTER, "invoice");

        assertThat(table1).isEqualTo("tenant1_invoice");
        assertThat(table2).isEqualTo("tenant2_invoice");
        assertThat(table1).isNotEqualTo(table2);

        List<FieldSchema> fields = List.of(new FieldSchema("amount", "number", false));
        ddlService.createTable("shared_starter", table1, fields);
        ddlService.createTable("shared_starter", table2, fields);

        UUID id1 = recordDao.insert("shared_starter", table1, fields, "tenant1", Map.of("amount", 1), "alice");
        UUID id2 = recordDao.insert("shared_starter", table2, fields, "tenant2", Map.of("amount", 2), "bob");

        assertThat(recordDao.findById("shared_starter", table1, "tenant1", id1)).isPresent();
        assertThat(recordDao.findById("shared_starter", table2, "tenant2", id2)).isPresent();
        // tenant1's record must not be visible through tenant2's physical table, and vice versa
        assertThat(recordDao.findById("shared_starter", table1, "tenant2", id1)).isEmpty();
    }

    @Test
    void createTable_rejectsInvalidEntityTypeIdentifier() {
        assertThatThrownBy(() -> ddlService.createTable("acme_platform", "Invoice-Type!",
                List.of(new FieldSchema("amount", "number", false))))
                .isInstanceOf(InvalidIdentifierException.class);
    }
}
