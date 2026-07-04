package com.platform.data.entity;

import com.platform.common.web.CursorPage;
import com.platform.common.web.StandardResponseEnvelope;
import com.platform.data.entity.domain.EntityDefinition;
import com.platform.data.entity.dto.CreateEntityDefinitionRequest;
import com.platform.data.entity.dto.UpdateEntityDefinitionRequest;
import com.platform.data.entity.dto.UpsertEntityRecordRequest;
import com.platform.data.entity.service.EntityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
public class EntityController {

    private final EntityService entityService;

    @PostMapping("/definitions")
    public ResponseEntity<StandardResponseEnvelope<EntityDefinition>> defineEntity(
            @Valid @RequestBody CreateEntityDefinitionRequest req, Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(entityService.defineEntity(req, auth.getName()), http));
    }

    @GetMapping("/definitions")
    public ResponseEntity<StandardResponseEnvelope<List<EntityDefinition>>> listDefinitions(HttpServletRequest http) {
        return ResponseEntity.ok(ok(entityService.listDefinitions(), http));
    }

    @PutMapping("/definitions/{entityType}")
    public ResponseEntity<StandardResponseEnvelope<EntityDefinition>> updateEntityDefinition(
            @PathVariable String entityType,
            @Valid @RequestBody UpdateEntityDefinitionRequest req, HttpServletRequest http) {
        return ResponseEntity.ok(ok(entityService.updateEntityDefinition(entityType, req), http));
    }

    @PostMapping("/{entityType}")
    public ResponseEntity<StandardResponseEnvelope<Map<String, Object>>> createRecord(
            @PathVariable String entityType,
            @Valid @RequestBody UpsertEntityRecordRequest req,
            Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(entityService.createRecord(entityType, req, auth.getName()), http));
    }

    @GetMapping("/{entityType}")
    public ResponseEntity<StandardResponseEnvelope<CursorPage<Map<String, Object>>>> listRecords(
            @PathVariable String entityType,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(entityService.listRecords(entityType, cursor, pageSize), http));
    }

    @GetMapping("/{entityType}/{id}")
    public ResponseEntity<StandardResponseEnvelope<Map<String, Object>>> getRecord(
            @PathVariable String entityType, @PathVariable UUID id, HttpServletRequest http) {
        return ResponseEntity.ok(ok(entityService.getRecord(entityType, id), http));
    }

    @PutMapping("/{entityType}/{id}")
    public ResponseEntity<StandardResponseEnvelope<Map<String, Object>>> updateRecord(
            @PathVariable String entityType, @PathVariable UUID id,
            @Valid @RequestBody UpsertEntityRecordRequest req, HttpServletRequest http) {
        return ResponseEntity.ok(ok(entityService.updateRecord(entityType, id, req), http));
    }

    private <T> StandardResponseEnvelope<T> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return StandardResponseEnvelope.of(data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId"));
    }
}
