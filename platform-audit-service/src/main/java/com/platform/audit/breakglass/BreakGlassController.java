package com.platform.audit.breakglass;

import com.platform.audit.chain.EventHashChain;
import com.platform.audit.event.AuditEvent;
import com.platform.audit.repository.AuditRepository;
import com.platform.common.web.StandardResponseEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class BreakGlassController {

    private final AuditRepository repo;

    /**
     * Records an emergency break-glass access event and returns the event ID and hash.
     *
     * This endpoint is restricted to SUPER_ADMIN role. The access itself is immediately
     * persisted as an immutable audit event — there is no way to invoke this endpoint
     * without leaving a verifiable trace in the hash chain.
     */
    @PostMapping("/break-glass")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<StandardResponseEnvelope<Map<String, Object>>> breakGlass(
            @Valid @RequestBody BreakGlassRequest request,
            HttpServletRequest http,
            Authentication auth) {

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("BREAK_GLASS_ACCESS")
                .domain("security")
                .tenantId(request.targetTenantId())
                .timestamp(Instant.now())
                .actorUserId(auth.getName())
                .actorRoles(List.of("SUPER_ADMIN"))
                .actorIp(http.getRemoteAddr())
                .resourceType(request.targetResourceType() != null ? request.targetResourceType() : "TENANT")
                .resourceId(request.targetResourceId() != null ? request.targetResourceId() : request.targetTenantId())
                .resourceName(request.targetTenantId())
                .operation("EMERGENCY_ACCESS")
                .prevState(null)
                .newState(null)
                .sourceService("platform-audit-service")
                .requestId(MDC.get("requestId"))
                .traceId(MDC.get("traceId"))
                .metadata(Map.of("reason", request.reason()))
                .build();

        String prevHash = repo.getLatestHash(event.getTenantId());
        if (prevHash == null) {
            prevHash = EventHashChain.genesis(event.getTenantId());
        }

        String eventHash = EventHashChain.compute(
                prevHash,
                event.getEventId(),
                event.getTenantId(),
                event.getTimestamp().toString(),
                event.getOperation(),
                event.getResourceId(),
                event.getActorUserId());

        repo.insert(event, eventHash, prevHash);
        repo.updateLatestHash(event.getTenantId(), eventHash);

        log.info("Break-glass access recorded: eventId={} actor={} tenant={}",
                event.getEventId(), event.getActorUserId(), event.getTenantId());

        Map<String, Object> responseData = Map.of(
                "eventId", event.getEventId(),
                "eventHash", eventHash,
                "timestamp", event.getTimestamp());

        return ResponseEntity.status(201).body(ok(responseData, http));
    }

    private StandardResponseEnvelope<Map<String, Object>> ok(Map<String, Object> data, HttpServletRequest http) {
        return StandardResponseEnvelope.of(data, MDC.get("requestId"), MDC.get("traceId"));
    }
}
