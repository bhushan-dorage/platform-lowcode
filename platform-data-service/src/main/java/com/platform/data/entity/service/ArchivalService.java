package com.platform.data.entity.service;

import com.platform.data.entity.domain.EntityRecord;
import com.platform.data.entity.repository.EntityRecordRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivalService {

    @Value("${archival.retention-days:90}")
    private int retentionDays;

    @Value("${archival.tenant-id:}")
    private String configuredTenantId;

    private final EntityRecordRepository recordRepo;
    private final MeterRegistry meterRegistry;

    /** Runs nightly at 02:00. Tenant-aware: configured per deployment via env. */
    @Scheduled(cron = "${archival.cron:0 0 2 * * *}")
    @Transactional
    public void runArchival() {
        if (configuredTenantId.isBlank()) {
            log.warn("Archival skipped: archival.tenant-id not configured");
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<EntityRecord> candidates = recordRepo.findForArchival(configuredTenantId, cutoff);
        if (candidates.isEmpty()) return;

        var ids = candidates.stream().map(EntityRecord::getId).collect(Collectors.toList());
        recordRepo.markArchived(ids, Instant.now());

        Counter.builder("data.archival.records")
                .tag("tenantId", configuredTenantId)
                .register(meterRegistry)
                .increment(ids.size());

        log.info("Archived {} records for tenantId={} olderThan={}", ids.size(), configuredTenantId, cutoff);
    }

    /** Manual trigger for a specific tenant (called from admin endpoint). */
    @Transactional
    public int archiveTenant(String tenantId, int retentionDaysOverride) {
        Instant cutoff = Instant.now().minus(retentionDaysOverride, ChronoUnit.DAYS);
        List<EntityRecord> candidates = recordRepo.findForArchival(tenantId, cutoff);
        if (candidates.isEmpty()) return 0;
        var ids = candidates.stream().map(EntityRecord::getId).collect(Collectors.toList());
        recordRepo.markArchived(ids, Instant.now());
        log.info("Manual archival: {} records tenantId={}", ids.size(), tenantId);
        return ids.size();
    }
}
