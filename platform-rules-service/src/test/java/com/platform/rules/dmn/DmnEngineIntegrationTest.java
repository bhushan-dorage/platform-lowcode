package com.platform.rules.dmn;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates, against a real Postgres, the specific empirical risk this effort's plan flagged:
 * whether Flowable's embedded DMN engine correctly self-provisions its ACT_DMN_* schema and
 * successfully deploys/executes a decision when pointed at an arbitrary, non-default schema —
 * simulating a dynamically-selected tenant schema, since neither this repo nor the earlier
 * BPMN bundle-deploy work has ever exercised this against a real database before.
 *
 * Uses Flowable's standalone DmnEngineConfiguration directly (bypassing the full Spring Boot
 * context) to isolate this question from unrelated startup concerns (Kafka, OAuth2 issuer
 * reachability) that a full @SpringBootTest would otherwise drag in.
 */
@Testcontainers
class DmnEngineIntegrationTest {

    private static final String TENANT_SCHEMA = "tenant_acme";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static DmnEngine dmnEngine;

    @BeforeAll
    static void setUp() throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + TENANT_SCHEMA);
        }

        dmnEngine = DmnEngineConfiguration.createStandaloneDmnEngineConfiguration()
                .setJdbcUrl(POSTGRES.getJdbcUrl())
                .setJdbcDriver(POSTGRES.getDriverClassName())
                .setJdbcUsername(POSTGRES.getUsername())
                .setJdbcPassword(POSTGRES.getPassword())
                .setDatabaseSchema(TENANT_SCHEMA)
                .setDatabaseSchemaUpdate(DmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .buildDmnEngine();
    }

    @AfterAll
    static void tearDown() {
        if (dmnEngine != null) {
            dmnEngine.close();
        }
    }

    @Test
    void deploysAndExecutesDecisionInsideDynamicallySelectedTenantSchema() throws Exception {
        String tenantId = "acme";
        DmnRepositoryService repositoryService = dmnEngine.getDmnRepositoryService();
        DmnDecisionService decisionService = dmnEngine.getDmnDecisionService();

        String dmnXml;
        try (InputStream in = getClass().getResourceAsStream("/dmn/loan-eligibility.dmn")) {
            dmnXml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        repositoryService.createDeployment()
                .name("test-deployment")
                .tenantId(tenantId)
                .addString("loan-eligibility.dmn", dmnXml)
                .deploy();

        List<Map<String, Object>> approved = decisionService.createExecuteDecisionBuilder()
                .decisionKey("loan-eligibility")
                .tenantId(tenantId)
                .variable("income", 50000)
                .executeDecision();

        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).get("approved")).isEqualTo(true);

        List<Map<String, Object>> rejected = decisionService.createExecuteDecisionBuilder()
                .decisionKey("loan-eligibility")
                .tenantId(tenantId)
                .variable("income", 10000)
                .executeDecision();

        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).get("approved")).isEqualTo(false);
    }
}
