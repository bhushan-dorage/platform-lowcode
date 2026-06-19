package com.platform.studio.artifact.controller;

import com.platform.common.web.StandardResponseEnvelope;
import com.platform.studio.artifact.domain.ArtifactType;
import com.platform.studio.artifact.dto.*;
import com.platform.studio.artifact.service.ArtifactService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    @PostMapping
    public ResponseEntity<StandardResponseEnvelope<ArtifactDto>> save(
            @RequestBody SaveArtifactRequest req,
            Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ok(artifactService.save(req, auth.getName()), http));
    }

    @GetMapping
    public ResponseEntity<StandardResponseEnvelope<List<ArtifactDto>>> list(
            @RequestParam(required = false) ArtifactType type, HttpServletRequest http) {
        return ResponseEntity.ok(ok(artifactService.list(type), http));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<StandardResponseEnvelope<ArtifactContentDto>> getContent(
            @PathVariable UUID id,
            @RequestParam(required = false) String ref,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(artifactService.getContent(id, ref), http));
    }

    @GetMapping("/{id}/versions/{version}/content")
    public ResponseEntity<StandardResponseEnvelope<ArtifactContentDto>> getVersionContent(
            @PathVariable UUID id, @PathVariable String version, HttpServletRequest http) {
        return ResponseEntity.ok(ok(artifactService.getPublishedContent(id, version), http));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<StandardResponseEnvelope<ArtifactDto>> publish(
            @PathVariable UUID id,
            @RequestBody PublishArtifactRequest req,
            Authentication auth, HttpServletRequest http) {
        return ResponseEntity.ok(ok(artifactService.publish(id, req.version(), auth.getName()), http));
    }

    @PostMapping("/{id}/deprecate")
    public ResponseEntity<StandardResponseEnvelope<ArtifactDto>> deprecate(
            @PathVariable UUID id, HttpServletRequest http) {
        return ResponseEntity.ok(ok(artifactService.deprecate(id), http));
    }

    private <T> StandardResponseEnvelope<T> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return StandardResponseEnvelope.of(data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId"));
    }
}
