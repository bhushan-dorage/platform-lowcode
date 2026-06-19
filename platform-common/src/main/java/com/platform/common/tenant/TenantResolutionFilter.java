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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves tenant identity from the X-Tenant-ID header and populates TenantContext
 * and MDC for the duration of the request.
 *
 * Runs at HIGHEST_PRECEDENCE so every downstream filter and servlet sees a valid context.
 * The finally block guarantees cleanup even when the downstream chain throws.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantResolutionFilter implements Filter {

    private static final String HEADER_TENANT_ID = "X-Tenant-ID";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String ATTR_REQUEST_ID = "requestId";

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

        String tenantId = httpReq.getHeader(HEADER_TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) {
            writeError(httpResp, HttpServletResponse.SC_BAD_REQUEST,
                    "MISSING_TENANT_ID", "X-Tenant-ID header is required", null);
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

    private void writeError(HttpServletResponse response, int status,
                            String code, String message, String traceId) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseEnvelope envelope = ErrorResponseEnvelope.of(code, message, traceId);
        objectMapper.writeValue(response.getOutputStream(), envelope);
    }
}
