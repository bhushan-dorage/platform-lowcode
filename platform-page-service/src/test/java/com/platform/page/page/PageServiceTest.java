package com.platform.page.page;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.page.exception.ResourceNotFoundException;
import com.platform.page.page.domain.PageDefinition;
import com.platform.page.page.domain.PageStatus;
import com.platform.page.page.dto.CreatePageRequest;
import com.platform.page.page.dto.PageDefinitionDto;
import com.platform.page.page.dto.PublishPageRequest;
import com.platform.page.page.repository.PageRepository;
import com.platform.page.page.service.PageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageServiceTest {

    @Mock
    private PageRepository pageRepository;

    @InjectMocks
    private PageService pageService;

    private static final String TENANT_ID = "tenant-test";

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID, TenantTier.PROFESSIONAL);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPage_savesAndReturnsDto() {
        CreatePageRequest req = new CreatePageRequest(
                "customer-dashboard",
                "Customer Dashboard",
                "Overview of customer KPIs",
                "{\"version\":\"1.0\",\"title\":\"Customer Dashboard\"}"
        );

        when(pageRepository.existsByTenantIdAndPageKey(TENANT_ID, "customer-dashboard")).thenReturn(false);

        PageDefinition saved = PageDefinition.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .pageKey("customer-dashboard")
                .name("Customer Dashboard")
                .description("Overview of customer KPIs")
                .schema("{\"version\":\"1.0\",\"title\":\"Customer Dashboard\"}")
                .status(PageStatus.DRAFT)
                .version(0)
                .createdBy("user@example.com")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(pageRepository.save(any(PageDefinition.class))).thenReturn(saved);

        PageDefinitionDto result = pageService.createPage(req, "user@example.com");

        assertThat(result).isNotNull();
        assertThat(result.pageKey()).isEqualTo("customer-dashboard");
        assertThat(result.name()).isEqualTo("Customer Dashboard");
        assertThat(result.tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.status()).isEqualTo(PageStatus.DRAFT);
        assertThat(result.version()).isEqualTo(0);
    }

    @Test
    void getPage_throwsWhenNotFound() {
        when(pageRepository.findByTenantIdAndPageKey(TENANT_ID, "nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pageService.getPage("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void publishPage_incrementsVersionAndSetsStatus() {
        UUID id = UUID.randomUUID();
        PageDefinition existing = PageDefinition.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .pageKey("customer-dashboard")
                .name("Customer Dashboard")
                .schema("{\"version\":\"1.0\"}")
                .status(PageStatus.DRAFT)
                .version(0)
                .createdBy("user@example.com")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        String newSchema = "{\"version\":\"1.1\",\"title\":\"Customer Dashboard v2\"}";
        PublishPageRequest req = new PublishPageRequest(newSchema);

        when(pageRepository.findByTenantIdAndPageKey(TENANT_ID, "customer-dashboard"))
                .thenReturn(Optional.of(existing));

        PageDefinition published = PageDefinition.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .pageKey("customer-dashboard")
                .name("Customer Dashboard")
                .schema(newSchema)
                .status(PageStatus.PUBLISHED)
                .version(1)
                .createdBy("user@example.com")
                .createdAt(existing.getCreatedAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(pageRepository.save(any(PageDefinition.class))).thenReturn(published);

        PageDefinitionDto result = pageService.publishPage("customer-dashboard", req, "publisher@example.com");

        assertThat(result.status()).isEqualTo(PageStatus.PUBLISHED);
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.schema()).isEqualTo(newSchema);
    }
}
