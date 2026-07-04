package com.platform.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

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
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAsJwt(String tenantId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("tenant_id", tenantId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, java.util.List.of()));
    }

    private static void authenticateAsJwtWithoutTenantClaim() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("roles", Map.of())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, java.util.List.of()));
    }

    @Test
    void missingClaimAndHeader_returns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(filterChain);
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void blankTenantIdHeader_noJwt_returns400() throws Exception {
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
    void jwtTenantClaim_setsTenantContextAndMdc() throws Exception {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);
        authenticateAsJwt("acme");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

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
    void jwtTenantClaim_takesPrecedenceOverHeader() throws Exception {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);
        authenticateAsJwt("acme");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "some-other-tenant");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("acme");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(tenantRegistry, never()).resolveTier("some-other-tenant");
    }

    @Test
    void noTenantClaimOnJwt_fallsBackToHeader() throws Exception {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);
        authenticateAsJwtWithoutTenantClaim();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("acme");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void noJwtAtAll_fallsBackToHeader() throws Exception {
        when(tenantRegistry.resolveTier("acme")).thenReturn(TenantTier.ENTERPRISE);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("acme");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void actuatorPath_bypassesTenantResolutionEntirely() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tenantRegistry);
        assertThat(TenantContext.get()).isNull();
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
