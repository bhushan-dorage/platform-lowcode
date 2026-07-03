package com.platform.data.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantContext;
import com.platform.common.web.CursorPage;
import com.platform.data.entity.domain.EntityDefinition;
import com.platform.data.entity.domain.EntityRecord;
import com.platform.data.entity.dto.CreateEntityDefinitionRequest;
import com.platform.data.entity.dto.UpsertEntityRecordRequest;
import com.platform.data.entity.repository.EntityDefinitionRepository;
import com.platform.data.entity.repository.EntityRecordRepository;
import com.platform.data.exception.ResourceNotFoundException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityService {

    private final EntityDefinitionRepository defRepo;
    private final EntityRecordRepository recordRepo;
    private final ObjectMapper objectMapper;

    @Timed(value = "data.entity.define")
    @Transactional
    public EntityDefinition defineEntity(CreateEntityDefinitionRequest req, String createdBy) {
        String tenantId = TenantContext.getTenantId();
        if (defRepo.existsByTenantIdAndEntityType(tenantId, req.entityType()))
            throw new IllegalArgumentException("Entity type already exists: " + req.entityType());
        EntityDefinition def = new EntityDefinition();
        def.setTenantId(tenantId);
        def.setEntityType(req.entityType());
        def.setDisplayName(req.displayName());
        def.setSchema(req.schema());
        def.setCreatedBy(createdBy);
        return defRepo.save(def);
    }

    @Timed(value = "data.entity.definitions.list")
    public List<EntityDefinition> listDefinitions() {
        return defRepo.findByTenantIdAndArchivedFalse(TenantContext.getTenantId());
    }

    @Timed(value = "data.entity.record.create")
    @Transactional
    public EntityRecord createRecord(String entityType, UpsertEntityRecordRequest req, String createdBy) {
        String tenantId = TenantContext.getTenantId();
        requireDefinition(tenantId, entityType);
        EntityRecord rec = new EntityRecord();
        rec.setTenantId(tenantId);
        rec.setEntityType(entityType);
        rec.setCreatedBy(createdBy);
        rec.setData(toJson(req.data()));
        return recordRepo.save(rec);
    }

    @Timed(value = "data.entity.record.get")
    public Map<String, Object> getRecord(String entityType, UUID id) {
        String tenantId = TenantContext.getTenantId();
        EntityRecord rec = recordRepo.findByIdAndTenantIdAndArchivedAtIsNull(id, tenantId)
                .filter(r -> r.getEntityType().equals(entityType))
                .orElseThrow(() -> new ResourceNotFoundException("Record not found: " + id));
        return fromJson(rec.getData());
    }

    @Timed(value = "data.entity.record.update")
    @Transactional
    public EntityRecord updateRecord(String entityType, UUID id, UpsertEntityRecordRequest req) {
        String tenantId = TenantContext.getTenantId();
        EntityRecord rec = recordRepo.findByIdAndTenantIdAndArchivedAtIsNull(id, tenantId)
                .filter(r -> r.getEntityType().equals(entityType))
                .orElseThrow(() -> new ResourceNotFoundException("Record not found: " + id));
        rec.setData(toJson(req.data()));
        return recordRepo.save(rec);
    }

    @Timed(value = "data.entity.record.list")
    public CursorPage<Map<String, Object>> listRecords(String entityType, String cursor, int pageSize) {
        String tenantId = TenantContext.getTenantId();
        requireDefinition(tenantId, entityType);

        // Fetch pageSize+1 rows at the DB level so we can determine hasMore without
        // loading the entire table into memory first.
        var limit = PageRequest.of(0, pageSize + 1);

        UUID cursorId = null;
        if (cursor != null) {
            try {
                cursorId = UUID.fromString(cursor);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid pagination cursor: " + cursor, ex);
            }
        }

        List<EntityRecord> results = cursorId != null
                ? recordRepo.findByTenantIdAndEntityTypeAndArchivedAtIsNullAndIdGreaterThanOrderByIdAsc(
                        tenantId, entityType, cursorId, limit)
                : recordRepo.findByTenantIdAndEntityTypeAndArchivedAtIsNullOrderByIdAsc(
                        tenantId, entityType, limit);

        boolean hasMore = results.size() > pageSize;
        List<EntityRecord> items = hasMore ? results.subList(0, pageSize) : results;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;
        List<Map<String, Object>> dtos = items.stream().map(r -> {
            Map<String, Object> m = fromJson(r.getData());
            m.put("_id", r.getId().toString());
            m.put("_createdAt", r.getCreatedAt().toString());
            return m;
        }).toList();
        return CursorPage.of(dtos, nextCursor, hasMore, pageSize);
    }

    private void requireDefinition(String tenantId, String entityType) {
        defRepo.findByTenantIdAndEntityTypeAndArchivedFalse(tenantId, entityType)
                .orElseThrow(() -> new ResourceNotFoundException("Entity type not found: " + entityType));
    }

    private String toJson(Map<String, Object> data) {
        try { return objectMapper.writeValueAsString(data); }
        catch (Exception e) { throw new RuntimeException("Serialization failed", e); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try { return objectMapper.readValue(json, Map.class); }
        catch (Exception e) { throw new RuntimeException("Deserialization failed", e); }
    }
}
