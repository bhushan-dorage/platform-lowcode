package com.platform.data.entity.service;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantRegistry;
import com.platform.data.entity.ddl.EntityTableDdlService;
import com.platform.data.entity.domain.EntityDefinition;
import com.platform.data.entity.repository.EntityDefinitionRepository;
import com.platform.data.entity.repository.EntityRecordDao;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivalService {

    @Value("${archival.retention-days:90}")
    private int retentionDays;

    @Value("${archival.tenant-id:}")
    private String configuredTenantId;

    private final EntityDefinitionRepository defRepo;
    private final EntityRecordDao recordDao;
    private final EntityTableDdlService ddlService;
    private final TenantRegistry tenantRegistry;
    private final MeterRegistry meterRegistry;

    /**
     * Runs nightly at 02:00. Tenant-aware: configured per deployment via env. This is a
     * scheduled background task, not an HTTP request, so TenantResolutionFilter never runs for
     * it — TenantContext must be set explicitly here (and cleared in finally), the same pattern
     * Kafka consumers elsewhere in this codebase use for the same reason.
     */
    @Scheduled(cron = "${archival.cron:0 0 2 * * *}")
    public void runArchival() {
        if (configuredTenantId.isBlank()) {
            log.warn("Archival skipped: archival.tenant-id not configured");
            return;
        }
        try {
            TenantContext.set(configuredTenantId, tenantRegistry.resolveTier(configuredTenantId));
            int archived = archiveTenant(configuredTenantId, retentionDays);
            Counter.builder("data.archival.records")
                    .tag("tenantId", configuredTenantId)
                    .register(meterRegistry)
                    .increment(archived);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Manual trigger for a specific tenant (called from admin endpoint). Every entity type is
     * now a separate physical table, so archival runs once per entity type instead of a single
     * bulk update against one shared table. Assumes TenantContext is already set to this
     * tenantId (as runArchival's caller does) — this method doesn't independently resolve
     * tier/schema for an arbitrary tenant.
     */
    @Transactional
    public int archiveTenant(String tenantId, int retentionDaysOverride) {
        Instant cutoff = Instant.now().minus(retentionDaysOverride, ChronoUnit.DAYS);
        String schema = TenantContext.getSchema();
        List<EntityDefinition> definitions = defRepo.findByTenantIdAndArchivedFalse(tenantId);

        int totalArchived = 0;
        for (EntityDefinition def : definitions) {
            String table = ddlService.physicalTableName(tenantId, TenantContext.getTier(), def.getEntityType());
            List<UUID> ids = recordDao.findIdsForArchival(schema, table, tenantId, cutoff);
            if (ids.isEmpty()) {
                continue;
            }
            recordDao.markArchived(schema, table, ids);
            totalArchived += ids.size();
            log.info("Archived {} records entityType={} tenantId={}", ids.size(), def.getEntityType(), tenantId);
        }
        return totalArchived;
    }
}
