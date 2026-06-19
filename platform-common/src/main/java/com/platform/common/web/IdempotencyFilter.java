package com.platform.common.web;

import com.platform.common.tenant.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Enforces idempotency on POST mutation endpoints.
 *
 * On the first request with a given Idempotency-Key the response body is stored in Redis
 * keyed as "{tenantId}:idempotency:{key}".  Subsequent requests with the same key within
 * the 24-hour TTL receive the cached response immediately, making it safe for clients to
 * retry without side effects.
 *
 * Only applies to POST requests that include the Idempotency-Key header; all other methods
 * and POST requests without the header pass through unchanged.
 */
@Component
public class IdempotencyFilter implements Filter {

    private static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        if (!HttpMethod.POST.matches(httpReq.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = httpReq.getHeader(HEADER_IDEMPOTENCY_KEY);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String tenantId = TenantContext.get() != null ? TenantContext.getTenantId() : "unknown";
        String cacheKey = tenantId + ":idempotency:" + idempotencyKey;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            httpResp.setStatus(HttpServletResponse.SC_OK);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(cached);
            return;
        }

        // Wrap the response so we can capture the body after the chain executes
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResp);
        try {
            chain.doFilter(request, responseWrapper);
        } finally {
            byte[] body = responseWrapper.getContentAsByteArray();
            if (body.length > 0 && responseWrapper.getStatus() < 300) {
                // Only cache successful responses — errors (4xx/5xx) should not be replayed
                String bodyStr = new String(body, responseWrapper.getCharacterEncoding());
                redisTemplate.opsForValue().set(cacheKey, bodyStr, TTL.toSeconds(), TimeUnit.SECONDS);
            }
            // Copy the buffered response to the actual response stream
            responseWrapper.copyBodyToResponse();
        }
    }
}
