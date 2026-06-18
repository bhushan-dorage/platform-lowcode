package com.platform.workflow.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimCheckService {

    private static final String REF_SUFFIX = "__ref";
    private static final Duration CLAIM_TTL = Duration.ofDays(7);

    @Value("${claimcheck.threshold-bytes:10240}")
    private int thresholdBytes;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * Returns a refId if value was stored (too large), null if caller should store directly.
     */
    public String store(String tenantId, String variableName, Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            if (bytes.length <= thresholdBytes) return null;
            String refId = UUID.randomUUID().toString();
            redis.opsForValue().set(claimKey(tenantId, refId), new String(bytes), CLAIM_TTL);
            log.debug("Claim check stored variableName={} refId={} size={}", variableName, refId, bytes.length);
            return refId;
        } catch (Exception e) {
            log.error("Claim check store failed for variableName={}", variableName, e);
            return null;
        }
    }

    public Object retrieve(String tenantId, String refId) {
        try {
            String json = redis.opsForValue().get(claimKey(tenantId, refId));
            if (json == null) {
                log.warn("Claim check miss for refId={}", refId);
                return null;
            }
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.error("Claim check retrieve failed for refId={}", refId, e);
            return null;
        }
    }

    /** Replaces `key__ref` entries with their resolved payloads. */
    public Map<String, Object> resolveVariables(String tenantId, Map<String, Object> raw) {
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getKey().endsWith(REF_SUFFIX)) {
                String originalKey = entry.getKey().substring(0, entry.getKey().length() - REF_SUFFIX.length());
                resolved.put(originalKey, retrieve(tenantId, (String) entry.getValue()));
            } else {
                resolved.put(entry.getKey(), entry.getValue());
            }
        }
        return resolved;
    }

    /** Converts large variables to `key__ref` entries for safe Flowable storage. */
    public Map<String, Object> prepareVariables(String tenantId, Map<String, Object> vars) {
        Map<String, Object> prepared = new HashMap<>();
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String refId = store(tenantId, entry.getKey(), entry.getValue());
            if (refId != null) {
                prepared.put(entry.getKey() + REF_SUFFIX, refId);
            } else {
                prepared.put(entry.getKey(), entry.getValue());
            }
        }
        return prepared;
    }

    private static String claimKey(String tenantId, String refId) {
        return tenantId + ":claimcheck:" + refId;
    }
}
