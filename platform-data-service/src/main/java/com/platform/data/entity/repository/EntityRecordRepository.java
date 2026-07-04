package com.platform.data.entity.repository;

import com.platform.data.entity.domain.EntityRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityRecordRepository extends JpaRepository<EntityRecord, UUID> {
    Optional<EntityRecord> findByIdAndTenantIdAndArchivedAtIsNull(UUID id, String tenantId);

    // Pageable parameter limits the SQL result set so the DB does not load all rows.
    // Pass PageRequest.of(0, pageSize + 1) to fetch exactly one more than needed for
    // the hasMore check without streaming the full table into application memory.
    List<EntityRecord> findByTenantIdAndEntityTypeAndArchivedAtIsNullAndIdGreaterThanOrderByIdAsc(
            String tenantId, String entityType, UUID cursor, Pageable pageable);

    List<EntityRecord> findByTenantIdAndEntityTypeAndArchivedAtIsNullOrderByIdAsc(
            String tenantId, String entityType, Pageable pageable);

    @Query("SELECT r FROM EntityRecord r WHERE r.tenantId = :tenantId AND r.archivedAt IS NULL AND r.createdAt < :cutoff")
    List<EntityRecord> findForArchival(String tenantId, Instant cutoff);

    @Modifying
    @Query("UPDATE EntityRecord r SET r.archivedAt = :now WHERE r.id IN :ids")
    void markArchived(List<UUID> ids, Instant now);
}
