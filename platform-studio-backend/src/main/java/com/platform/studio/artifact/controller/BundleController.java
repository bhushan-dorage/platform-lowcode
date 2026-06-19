package com.platform.studio.artifact.controller;

import com.platform.common.web.StandardResponseEnvelope;
import com.platform.studio.artifact.domain.DeploymentBundle;
import com.platform.studio.artifact.dto.CreateBundleRequest;
import com.platform.studio.artifact.service.BundleService;
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
@RequestMapping("/api/v1/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;

    @PostMapping
    public ResponseEntity<StandardResponseEnvelope<DeploymentBundle>> createBundle(
            @RequestBody CreateBundleRequest req, Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(bundleService.createBundle(req, auth.getName()), http));
    }

    @GetMapping
    public ResponseEntity<StandardResponseEnvelope<List<DeploymentBundle>>> listBundles(HttpServletRequest http) {
        return ResponseEntity.ok(ok(bundleService.listBundles(), http));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StandardResponseEnvelope<DeploymentBundle>> getBundle(
            @PathVariable UUID id, HttpServletRequest http) {
        return ResponseEntity.ok(ok(bundleService.getBundle(id), http));
    }

    @PostMapping("/{id}/deploy")
    public ResponseEntity<StandardResponseEnvelope<DeploymentBundle>> deploy(
            @PathVariable UUID id, Authentication auth, HttpServletRequest http) {
        return ResponseEntity.ok(ok(bundleService.deployBundle(id, auth.getName()), http));
    }

    private <T> StandardResponseEnvelope<T> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return StandardResponseEnvelope.of(data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId"));
    }
}
