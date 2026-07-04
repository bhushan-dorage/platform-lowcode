package com.platform.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.web.ErrorResponseEnvelope;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves tenant identity for the duration of the request and populates TenantContext and MDC.
 *
 * Prefers the verified {@code tenant_id} claim off the authenticated JWT (populated by Spring
 * Security's resource-server filter, which runs before this filter — see the FilterRegistrationBean
 * in PlatformCommonAutoConfiguration for the ordering). Falls back to the client-supplied
 * X-Tenant-ID header when the caller has no JWT tenant claim (e.g. internal service-to-service
 * calls using a client-credentials token), since there is no per-tenant service-account
 * provisioning today.
 *
 * The finally block guarantees TenantContext/MDC cleanup even when the downstream chain throws.
 */
public class TenantResolutionFilter implements Filter {

    private static final String HEADER_TENANT_ID = "X-Tenant-ID";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String ATTR_REQUEST_ID = "requestId";
    private static final String CLAIM_TENANT_ID = "tenant_id";

    private final TenantRegistry tenantRegistry;
    private final ObjectMapper objectMapper;

    public TenantResolutionFilter(TenantRegistry tenantRegistry, ObjectMapper objectMapper) {
        this.tenantRegistry = tenantRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        if (httpReq.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String tenantId = resolveTenantIdFromJwt();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = httpReq.getHeader(HEADER_TENANT_ID);
        }
        if (tenantId == null || tenantId.isBlank()) {
            writeError(httpResp, HttpServletResponse.SC_BAD_REQUEST,
                    "MISSING_TENANT_ID", "No tenant_id claim on the token and no X-Tenant-ID header provided", null);
            return;
        }

        TenantTier tier;
        try {
            tier = tenantRegistry.resolveTier(tenantId);
        } catch (TenantNotFoundException ex) {
            writeError(httpResp, HttpServletResponse.SC_BAD_REQUEST,
                    "UNKNOWN_TENANT", ex.getMessage(), null);
            return;
        }

        String requestId = httpReq.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Expose requestId as a request attribute so response-envelope builders can read it
        httpReq.setAttribute(ATTR_REQUEST_ID, requestId);

        TenantContext.set(tenantId, tier);
        MDC.put("tenantId", tenantId);
        MDC.put("requestId", requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Always clear — prevents ThreadLocal/MDC leaks on pooled threads
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private String resolveTenantIdFromJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString(CLAIM_TENANT_ID);
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int status,
                            String code, String message, String traceId) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseEnvelope envelope = ErrorResponseEnvelope.of(code, message, traceId);
        objectMapper.writeValue(response.getOutputStream(), envelope);
    }
}
