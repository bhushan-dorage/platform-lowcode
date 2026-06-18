package com.platform.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_returnsPopulatedContext() {
        TenantContext.set("acme", TenantTier.ENTERPRISE);

        TenantContext ctx = TenantContext.get();

        assertThat(ctx).isNotNull();
        assertThat(ctx.tenantId()).isEqualTo("acme");
        assertThat(ctx.tier()).isEqualTo(TenantTier.ENTERPRISE);
    }

    @Test
    void schema_enterprise_isDedicatedSchema() {
        TenantContext.set("acme", TenantTier.ENTERPRISE);

        assertThat(TenantContext.getSchema()).isEqualTo("acme_platform");
    }

    @Test
    void schema_professional_isDedicatedSchema() {
        TenantContext.set("globex", TenantTier.PROFESSIONAL);

        assertThat(TenantContext.getSchema()).isEqualTo("globex_platform");
    }

    @Test
    void schema_starter_isSharedSchema() {
        TenantContext.set("smallco", TenantTier.STARTER);

        assertThat(TenantContext.getSchema()).isEqualTo("shared_starter");
    }

    @Test
    void kafkaPrefix_isDerivedFromTenantId() {
        TenantContext.set("acme", TenantTier.ENTERPRISE);

        assertThat(TenantContext.getKafkaPrefix()).isEqualTo("acme.");
    }

    @Test
    void clear_preventsThreadLocalLeak() {
        TenantContext.set("acme", TenantTier.ENTERPRISE);
        TenantContext.clear();

        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void staticAccessors_throwWhenContextAbsent() {
        // Context was cleared in @AfterEach / never set in this test
        assertThatThrownBy(TenantContext::getTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TenantContext is not set");
    }

    @Test
    void set_overwritesPreviousContext() {
        TenantContext.set("acme", TenantTier.ENTERPRISE);
        TenantContext.set("beta", TenantTier.STARTER);

        assertThat(TenantContext.getTenantId()).isEqualTo("beta");
        assertThat(TenantContext.getSchema()).isEqualTo("shared_starter");
    }
}
