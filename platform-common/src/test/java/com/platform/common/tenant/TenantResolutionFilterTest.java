package com.platform.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

    @Mock
    private TenantRegistry tenantRegistry;

    @Mock
    private FilterChain filterChain;

    private TenantResolutionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantResolutionFilter(tenantRegistry, new ObjectMapper());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    void missingTenantIdHeader_returns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(filterChain);
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void blankTenantIdHeader_returns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(filterChain);
    }

    @Test
    void unknownTenant_returns400() throws Exception {
        when(tenantRegistry.resolveTier("unknown-tenant"))
                .thenThrow(new TenantNotFoundException("unknown-tenant"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "unknown-tenant");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(filterChain);
    }

    @Test
    void validHeader_setsTenantContextAndMdc() throws Exception {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture context state from inside the chain while it is active
        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("acme");
            assertThat(TenantContext.getTier()).isEqualTo(TenantTier.ENTERPRISE);
            assertThat(MDC.get("tenantId")).isEqualTo("acme");
            assertThat(MDC.get("requestId")).isNotBlank();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void requestIdHeader_propagatedToMdc() throws Exception {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "acme");
        request.addHeader("X-Request-ID", "req-abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("requestId")).isEqualTo("req-abc-123");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);
    }

    @Test
    void tenantContextClearedInFinally_whenDownstreamThrows() throws Exception {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new RuntimeException("downstream failure"))
                .when(filterChain).doFilter(any(), any());

        try {
            filter.doFilter(request, response, filterChain);
        } catch (RuntimeException ignored) {
            // Expected — we only care that context was cleaned up
        }

        assertThat(TenantContext.get()).isNull();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }
}
