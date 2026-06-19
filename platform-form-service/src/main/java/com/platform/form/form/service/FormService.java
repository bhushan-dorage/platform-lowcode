package com.platform.form.form.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantContext;
import com.platform.common.web.CursorPage;
import com.platform.form.form.domain.*;
import com.platform.form.form.dto.*;
import com.platform.form.form.repository.*;
import com.platform.form.form.validation.JsonSchemaValidator;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormService {

    private final FormDefinitionRepository formRepo;
    private final FormVersionRepository versionRepo;
    private final FormSubmissionRepository submissionRepo;
    private final JsonSchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;

    @Timed(value = "form.definition.create")
    @Transactional
    public FormDefinitionDto createForm(CreateFormRequest req, String createdBy) {
        String tenantId = TenantContext.getTenantId();
        if (formRepo.existsByTenantIdAndFormKey(tenantId, req.formKey()))
            throw new IllegalArgumentException("Form key already exists: " + req.formKey());

        schemaValidator.assertValidSchema(req.jsonSchema());

        FormDefinition def = new FormDefinition();
        def.setTenantId(tenantId);
        def.setFormKey(req.formKey());
        def.setName(req.name());
        def.setDescription(req.description());
        def.setCreatedBy(createdBy);
        def = formRepo.save(def);

        FormVersion ver = new FormVersion();
        ver.setFormDefinitionId(def.getId());
        ver.setTenantId(tenantId);
        ver.setVersion(1);
        ver.setJsonSchema(req.jsonSchema());
        ver.setUiSchema(req.uiSchema());
        ver.setPublishedBy(createdBy);
        ver.setPublishedAt(Instant.now());
        versionRepo.save(ver);

        def.setCurrentVersion(1);
        def.setStatus(FormStatus.ACTIVE);
        return toDto(formRepo.save(def));
    }

    @Timed(value = "form.definition.list")
    public CursorPage<FormDefinitionDto> listForms(String cursor, int pageSize) {
        String tenantId = TenantContext.getTenantId();
        List<FormDefinition> results = cursor != null
                ? formRepo.findByTenantIdAndIdGreaterThanOrderByIdAsc(tenantId, UUID.fromString(cursor))
                : formRepo.findByTenantIdOrderByIdAsc(tenantId);

        // Limit to pageSize+1 in-memory (Spring Data doesn't support Pageable+id-cursor cleanly without @Query)
        List<FormDefinition> page = results.stream().limit(pageSize + 1L).toList();
        boolean hasMore = page.size() > pageSize;
        List<FormDefinition> items = hasMore ? page.subList(0, pageSize) : page;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;
        return CursorPage.of(items.stream().map(this::toDto).toList(), nextCursor, hasMore, pageSize);
    }

    @Timed(value = "form.definition.get")
    public FormDefinitionDto getForm(String formKey) {
        return toDto(requireForm(formKey));
    }

    @Timed(value = "form.version.publish")
    @Transactional
    public FormVersion publishVersion(String formKey, PublishVersionRequest req, String publishedBy) {
        FormDefinition def = requireForm(formKey);
        schemaValidator.assertValidSchema(req.jsonSchema());

        int nextVersion = def.getCurrentVersion() + 1;
        FormVersion ver = new FormVersion();
        ver.setFormDefinitionId(def.getId());
        ver.setTenantId(def.getTenantId());
        ver.setVersion(nextVersion);
        ver.setJsonSchema(req.jsonSchema());
        ver.setUiSchema(req.uiSchema());
        ver.setPublishedBy(publishedBy);
        ver.setPublishedAt(Instant.now());
        versionRepo.save(ver);

        def.setCurrentVersion(nextVersion);
        formRepo.save(def);
        log.info("Published form version formKey={} version={}", formKey, nextVersion);
        return ver;
    }

    @Timed(value = "form.submission.validate")
    public ValidationResult validateSubmission(String formKey, java.util.Map<String, Object> data) {
        FormDefinition def = requireForm(formKey);
        FormVersion ver = versionRepo
                .findByFormDefinitionIdAndTenantIdAndVersion(def.getId(), def.getTenantId(), def.getCurrentVersion())
                .orElseThrow(() -> new IllegalStateException("No active version for form: " + formKey));
        return schemaValidator.validate(ver.getJsonSchema(), data);
    }

    @Timed(value = "form.submission.submit")
    @Transactional
    public FormSubmission submitForm(String formKey, FormSubmissionRequest req, String submittedBy) {
        FormDefinition def = requireForm(formKey);
        ValidationResult result = validateSubmission(formKey, req.data());
        if (!result.valid()) throw new com.platform.form.exception.FormValidationException(result.errors());

        FormSubmission sub = new FormSubmission();
        sub.setFormDefinitionId(def.getId());
        sub.setTenantId(def.getTenantId());
        sub.setFormVersion(def.getCurrentVersion());
        sub.setTaskId(req.taskId());
        sub.setProcessInstanceId(req.processInstanceId());
        sub.setSubmittedBy(submittedBy);
        try { sub.setData(objectMapper.writeValueAsString(req.data())); }
        catch (Exception e) { throw new RuntimeException("Failed to serialize form data", e); }
        return submissionRepo.save(sub);
    }

    @Timed(value = "form.submission.list")
    public CursorPage<FormSubmission> listSubmissions(String formKey, String cursor, int pageSize) {
        FormDefinition def = requireForm(formKey);
        List<FormSubmission> results = cursor != null
                ? submissionRepo.findByFormDefinitionIdAndTenantIdAndIdGreaterThanOrderByIdAsc(
                        def.getId(), def.getTenantId(), UUID.fromString(cursor))
                : submissionRepo.findByFormDefinitionIdAndTenantIdOrderByIdAsc(def.getId(), def.getTenantId());

        List<FormSubmission> page = results.stream().limit(pageSize + 1L).toList();
        boolean hasMore = page.size() > pageSize;
        List<FormSubmission> items = hasMore ? page.subList(0, pageSize) : page;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;
        return CursorPage.of(items, nextCursor, hasMore, pageSize);
    }

    private FormDefinition requireForm(String formKey) {
        return formRepo.findByTenantIdAndFormKey(TenantContext.getTenantId(), formKey)
                .orElseThrow(() -> new com.platform.form.exception.ResourceNotFoundException("Form not found: " + formKey));
    }

    private FormDefinitionDto toDto(FormDefinition d) {
        return new FormDefinitionDto(d.getId(), d.getTenantId(), d.getFormKey(), d.getName(),
                d.getDescription(), d.getCurrentVersion(), d.getStatus(), d.getCreatedBy(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
