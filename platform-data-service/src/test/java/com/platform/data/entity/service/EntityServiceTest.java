package com.platform.data.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityServiceTest {

    @Mock EntityDefinitionRepository defRepo;
    @Mock EntityRecordDao recordDao;
    @Mock EntityTableDdlService ddlService;

    JsonSchemaParser jsonSchemaParser;
    SchemaDiff schemaDiff;
    EntityService entityService;

    @BeforeEach
    void setup() {
        jsonSchemaParser = new JsonSchemaParser(new ObjectMapper());
        schemaDiff = new SchemaDiff();
        entityService = new EntityService(defRepo, recordDao, jsonSchemaParser, schemaDiff, ddlService);
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private EntityDefinition definition(String entityType, String schemaJson) {
        EntityDefinition def = new EntityDefinition();
        def.setTenantId("acme");
        def.setEntityType(entityType);
        def.setDisplayName(entityType);
        def.setSchema(schemaJson);
        return def;
    }

    @Test
    void defineEntity_createsPhysicalTableThenSavesDefinition() {
        when(defRepo.existsByTenantIdAndEntityType("acme", "invoice")).thenReturn(false);
        when(ddlService.physicalTableName("acme", TenantTier.PROFESSIONAL, "invoice")).thenReturn("invoice");
        when(defRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String schema = "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"}},\"required\":[\"amount\"]}";
        EntityDefinition result = entityService.defineEntity(
                new CreateEntityDefinitionRequest("invoice", "Invoice", schema), "alice");

        verify(ddlService).createTable(eq("acme_platform"), eq("invoice"), any());
        assertThat(result.getEntityType()).isEqualTo("invoice");
    }

    @Test
    void defineEntity_alreadyExists_throwsIllegalArgument() {
        when(defRepo.existsByTenantIdAndEntityType("acme", "invoice")).thenReturn(true);

        assertThatThrownBy(() -> entityService.defineEntity(
                new CreateEntityDefinitionRequest("invoice", "Invoice", "{\"type\":\"object\"}"), "alice"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(ddlService);
    }

    @Test
    void updateEntityDefinition_additiveChange_altersTableAndSaves() {
        EntityDefinition existing = definition("invoice", "{\"type\":\"object\",\"properties\":{}}");
        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "invoice"))
                .thenReturn(Optional.of(existing));
        when(ddlService.physicalTableName("acme", TenantTier.PROFESSIONAL, "invoice")).thenReturn("invoice");
        when(defRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String newSchema = "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"}}}";
        EntityDefinition updated = entityService.updateEntityDefinition(
                "invoice", new UpdateEntityDefinitionRequest("Invoice v2", newSchema));

        verify(ddlService).applyAdditiveAlter(eq("acme_platform"), eq("invoice"), argThat(added ->
                added.size() == 1 && added.get(0).name().equals("amount")));
        assertThat(updated.getSchema()).isEqualTo(newSchema);
    }

    @Test
    void updateEntityDefinition_removedProperty_throwsNonAdditive() {
        EntityDefinition existing = definition("invoice",
                "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"}}}");
        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "invoice"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> entityService.updateEntityDefinition(
                "invoice", new UpdateEntityDefinitionRequest("Invoice", "{\"type\":\"object\",\"properties\":{}}")))
                .isInstanceOf(NonAdditiveSchemaChangeException.class);
        verifyNoInteractions(ddlService);
    }

    @Test
    void updateEntityDefinition_unknownEntityType_throwsNotFound() {
        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entityService.updateEntityDefinition(
                "missing", new UpdateEntityDefinitionRequest("X", "{\"type\":\"object\",\"properties\":{}}")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRecord_unknownField_throwsIllegalArgument() {
        EntityDefinition existing = definition("invoice",
                "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"}}}");
        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "invoice"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> entityService.createRecord(
                "invoice", new UpsertEntityRecordRequest(Map.of("nope", 1)), "alice"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(recordDao);
    }

    @Test
    void createRecord_validField_insertsAndReturnsEnrichedRecord() {
        EntityDefinition existing = definition("invoice",
                "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"}}}");
        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "invoice"))
                .thenReturn(Optional.of(existing));
        when(ddlService.physicalTableName("acme", TenantTier.PROFESSIONAL, "invoice")).thenReturn("invoice");

        UUID id = UUID.randomUUID();
        when(recordDao.insert(eq("acme_platform"), eq("invoice"), any(), eq("acme"), any(), eq("alice")))
                .thenReturn(id);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("amount", 100);
        row.put("created_at", Timestamp.from(Instant.now()));
        row.put("updated_at", Timestamp.from(Instant.now()));
        when(recordDao.findById("acme_platform", "invoice", "acme", id)).thenReturn(Optional.of(row));

        Map<String, Object> result = entityService.createRecord(
                "invoice", new UpsertEntityRecordRequest(Map.of("amount", 100)), "alice");

        assertThat(result.get("amount")).isEqualTo(100);
        assertThat(result.get("_id")).isEqualTo(id.toString());
        assertThat(result).containsKeys("_createdAt", "_updatedAt");
    }

    @Test
    void getRecord_notFound_throwsResourceNotFound() {
        EntityDefinition existing = definition("invoice",
                "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"}}}");
        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "invoice"))
                .thenReturn(Optional.of(existing));
        when(ddlService.physicalTableName("acme", TenantTier.PROFESSIONAL, "invoice")).thenReturn("invoice");

        UUID id = UUID.randomUUID();
        when(recordDao.findById("acme_platform", "invoice", "acme", id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entityService.getRecord("invoice", id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
