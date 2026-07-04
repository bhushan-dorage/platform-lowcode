package com.platform.data.entity.service;

import com.platform.common.tenant.TenantContext;
import com.platform.common.web.CursorPage;
import com.platform.data.entity.ddl.EntityTableDdlService;
import com.platform.data.entity.ddl.FieldSchema;
import com.platform.data.entity.ddl.JsonSchemaParser;
import com.platform.data.entity.ddl.SchemaDiff;
import com.platform.data.entity.domain.EntityDefinition;
import com.platform.data.entity.dto.CreateEntityDefinitionRequest;
import com.platform.data.entity.dto.UpdateEntityDefinitionRequest;
import com.platform.data.entity.dto.UpsertEntityRecordRequest;
import com.platform.data.entity.repository.EntityDefinitionRepository;
import com.platform.data.entity.repository.EntityRecordDao;
import com.platform.data.exception.NonAdditiveSchemaChangeException;
import com.platform.data.exception.ResourceNotFoundException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityService {

    private final EntityDefinitionRepository defRepo;
    private final EntityRecordDao recordDao;
    private final JsonSchemaParser jsonSchemaParser;
    private final SchemaDiff schemaDiff;
    private final EntityTableDdlService ddlService;

    @Timed(value = "data.entity.define")
    @Transactional
    public EntityDefinition defineEntity(CreateEntityDefinitionRequest req, String createdBy) {
        String tenantId = TenantContext.getTenantId();
        if (defRepo.existsByTenantIdAndEntityType(tenantId, req.entityType()))
            throw new IllegalArgumentException("Entity type already exists: " + req.entityType());

        List<FieldSchema> fields = jsonSchemaParser.parse(req.schema());
        String table = ddlService.physicalTableName(tenantId, TenantContext.getTier(), req.entityType());
        ddlService.createTable(TenantContext.getSchema(), table, fields);

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

    @Timed(value = "data.entity.definition.update")
    @Transactional
    public EntityDefinition updateEntityDefinition(String entityType, UpdateEntityDefinitionRequest req) {
        String tenantId = TenantContext.getTenantId();
        EntityDefinition def = defRepo.findByTenantIdAndEntityTypeAndArchivedFalse(tenantId, entityType)
                .orElseThrow(() -> new ResourceNotFoundException("Entity type not found: " + entityType));

        List<FieldSchema> oldFields = jsonSchemaParser.parse(def.getSchema());
        List<FieldSchema> newFields = jsonSchemaParser.parse(req.schema());
        SchemaDiff.SchemaDiffResult diff = schemaDiff.diff(oldFields, newFields);
        if (!diff.isAdditive()) {
            throw new NonAdditiveSchemaChangeException(
                    diff.removed(), diff.typeChanged(), diff.newlyRequiredExisting());
        }

        String table = ddlService.physicalTableName(tenantId, TenantContext.getTier(), entityType);
        ddlService.applyAdditiveAlter(TenantContext.getSchema(), table, diff.added());

        def.setDisplayName(req.displayName());
        def.setSchema(req.schema());
        return defRepo.save(def);
    }

    @Timed(value = "data.entity.record.create")
    @Transactional
    public Map<String, Object> createRecord(String entityType, UpsertEntityRecordRequest req, String createdBy) {
        String tenantId = TenantContext.getTenantId();
        EntityDefinition def = requireDefinition(tenantId, entityType);
        List<FieldSchema> fields = jsonSchemaParser.parse(def.getSchema());
        assertKnownFields(fields, req.data());

        String table = ddlService.physicalTableName(tenantId, TenantContext.getTier(), entityType);
        UUID id = recordDao.insert(TenantContext.getSchema(), table, fields, tenantId, req.data(), createdBy);
        return getRecord(entityType, id);
    }

    @Timed(value = "data.entity.record.get")
    public Map<String, Object> getRecord(String entityType, UUID id) {
        String tenantId = TenantContext.getTenantId();
        EntityDefinition def = requireDefinition(tenantId, entityType);
        List<FieldSchema> fields = jsonSchemaParser.parse(def.getSchema());
        String table = ddlService.physicalTableName(tenantId, TenantContext.getTier(), entityType);

        Map<String, Object> row = recordDao.findById(TenantContext.getSchema(), table, tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found: " + id));
        return toRecordDto(row, fields);
    }

    @Timed(value = "data.entity.record.update")
    @Transactional
    public Map<String, Object> updateRecord(String entityType, UUID id, UpsertEntityRecordRequest req) {
        String tenantId = TenantContext.getTenantId();
        EntityDefinition def = requireDefinition(tenantId, entityType);
        List<FieldSchema> fields = jsonSchemaParser.parse(def.getSchema());
        assertKnownFields(fields, req.data());

        String table = ddlService.physicalTableName(tenantId, TenantContext.getTier(), entityType);
        int updated = recordDao.update(TenantContext.getSchema(), table, fields, tenantId, id, req.data());
        if (updated == 0) {
            throw new ResourceNotFoundException("Record not found: " + id);
        }
        return getRecord(entityType, id);
    }

    @Timed(value = "data.entity.record.list")
    public CursorPage<Map<String, Object>> listRecords(String entityType, String cursor, int pageSize) {
        String tenantId = TenantContext.getTenantId();
        EntityDefinition def = requireDefinition(tenantId, entityType);
        List<FieldSchema> fields = jsonSchemaParser.parse(def.getSchema());
        String table = ddlService.physicalTableName(tenantId, TenantContext.getTier(), entityType);

        UUID cursorId = null;
        if (cursor != null) {
            try {
                cursorId = UUID.fromString(cursor);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid pagination cursor: " + cursor, ex);
            }
        }

        // pageSize+1 fetch at the DB level so hasMore is known without loading the whole table.
        List<Map<String, Object>> rows = recordDao.findPage(TenantContext.getSchema(), table, tenantId, cursorId, pageSize);
        boolean hasMore = rows.size() > pageSize;
        List<Map<String, Object>> items = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = hasMore ? items.get(items.size() - 1).get("id").toString() : null;

        List<Map<String, Object>> dtos = items.stream().map(row -> toRecordDto(row, fields)).toList();
        return CursorPage.of(dtos, nextCursor, hasMore, pageSize);
    }

    private EntityDefinition requireDefinition(String tenantId, String entityType) {
        return defRepo.findByTenantIdAndEntityTypeAndArchivedFalse(tenantId, entityType)
                .orElseThrow(() -> new ResourceNotFoundException("Entity type not found: " + entityType));
    }

    /** Record payloads may only set known entity fields — unknown keys would otherwise become arbitrary column names. */
    private void assertKnownFields(List<FieldSchema> fields, Map<String, Object> data) {
        Set<String> known = fields.stream().map(FieldSchema::name).collect(Collectors.toSet());
        for (String key : data.keySet()) {
            if (!known.contains(key)) {
                throw new IllegalArgumentException("Unknown field for this entity type: " + key);
            }
        }
    }

    private Map<String, Object> toRecordDto(Map<String, Object> row, List<FieldSchema> fields) {
        Map<String, Object> dto = new LinkedHashMap<>();
        for (FieldSchema field : fields) {
            dto.put(field.name(), row.get(field.name()));
        }
        dto.put("_id", row.get("id").toString());
        dto.put("_createdAt", toIsoString(row.get("created_at")));
        dto.put("_updatedAt", toIsoString(row.get("updated_at")));
        return dto;
    }

    private String toIsoString(Object timestamp) {
        return timestamp instanceof Timestamp ts ? ts.toInstant().toString() : String.valueOf(timestamp);
    }
}
