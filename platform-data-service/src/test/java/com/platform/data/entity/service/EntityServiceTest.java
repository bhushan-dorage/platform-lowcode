package com.platform.data.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.data.entity.domain.EntityDefinition;
import com.platform.data.entity.dto.UpdateEntityDefinitionRequest;
import com.platform.data.entity.repository.EntityDefinitionRepository;
import com.platform.data.entity.repository.EntityRecordRepository;
import com.platform.data.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityServiceTest {

    @Mock EntityDefinitionRepository defRepo;
    @Mock EntityRecordRepository recordRepo;

    EntityService entityService;

    @BeforeEach
    void setup() {
        entityService = new EntityService(defRepo, recordRepo, new ObjectMapper());
        TenantContext.set("acme", TenantTier.PROFESSIONAL);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void updateEntityDefinition_updatesDisplayNameAndSchema() {
        EntityDefinition existing = new EntityDefinition();
        existing.setTenantId("acme");
        existing.setEntityType("invoice");
        existing.setDisplayName("Invoice");
        existing.setSchema("{\"type\":\"object\"}");

        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "invoice"))
                .thenReturn(Optional.of(existing));
        when(defRepo.save(any())).thenReturn(existing);

        EntityDefinition updated = entityService.updateEntityDefinition(
                "invoice", new UpdateEntityDefinitionRequest("Invoice v2", "{\"type\":\"object\",\"properties\":{}}"));

        assertThat(updated.getDisplayName()).isEqualTo("Invoice v2");
        assertThat(updated.getSchema()).isEqualTo("{\"type\":\"object\",\"properties\":{}}");
    }

    @Test
    void updateEntityDefinition_unknownEntityType_throwsNotFound() {
        when(defRepo.findByTenantIdAndEntityTypeAndArchivedFalse("acme", "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entityService.updateEntityDefinition(
                "missing", new UpdateEntityDefinitionRequest("X", "{}")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
