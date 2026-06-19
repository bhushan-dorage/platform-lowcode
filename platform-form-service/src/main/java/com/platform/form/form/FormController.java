package com.platform.form.form;

import com.platform.common.web.CursorPage;
import com.platform.common.web.StandardResponseEnvelope;
import com.platform.form.form.domain.FormSubmission;
import com.platform.form.form.domain.FormVersion;
import com.platform.form.form.dto.*;
import com.platform.form.form.service.FormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    @PostMapping
    public ResponseEntity<StandardResponseEnvelope<FormDefinitionDto>> createForm(
            @Valid @RequestBody CreateFormRequest req, Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(formService.createForm(req, auth.getName()), http));
    }

    @GetMapping
    public ResponseEntity<StandardResponseEnvelope<CursorPage<FormDefinitionDto>>> listForms(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(formService.listForms(cursor, pageSize), http));
    }

    @GetMapping("/{formKey}")
    public ResponseEntity<StandardResponseEnvelope<FormDefinitionDto>> getForm(
            @PathVariable String formKey, HttpServletRequest http) {
        return ResponseEntity.ok(ok(formService.getForm(formKey), http));
    }

    @PostMapping("/{formKey}/versions")
    public ResponseEntity<StandardResponseEnvelope<FormVersion>> publishVersion(
            @PathVariable String formKey,
            @Valid @RequestBody PublishVersionRequest req,
            Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(formService.publishVersion(formKey, req, auth.getName()), http));
    }

    @PostMapping("/{formKey}/validate")
    public ResponseEntity<StandardResponseEnvelope<ValidationResult>> validate(
            @PathVariable String formKey,
            @RequestBody Map<String, Object> data,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(formService.validateSubmission(formKey, data), http));
    }

    @PostMapping("/{formKey}/submissions")
    public ResponseEntity<StandardResponseEnvelope<FormSubmission>> submit(
            @PathVariable String formKey,
            @Valid @RequestBody FormSubmissionRequest req,
            Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(formService.submitForm(formKey, req, auth.getName()), http));
    }

    @GetMapping("/{formKey}/submissions")
    public ResponseEntity<StandardResponseEnvelope<CursorPage<FormSubmission>>> listSubmissions(
            @PathVariable String formKey,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(formService.listSubmissions(formKey, cursor, pageSize), http));
    }

    private <T> StandardResponseEnvelope<T> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return StandardResponseEnvelope.of(data, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId"));
    }
}
