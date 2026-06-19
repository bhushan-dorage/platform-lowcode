package com.platform.data.hasura;

import com.platform.common.tenant.TenantContext;
import com.platform.data.entity.domain.EntityDefinition;
import com.platform.data.entity.repository.EntityDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates Hasura metadata for tracked entity tables.
 * Output is applied via Hasura Metadata API (POST /v1/metadata).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HasuraConfigGenerator {

    private final EntityDefinitionRepository defRepo;

    public Map<String, Object> generateMetadata(String tenantId) {
        List<EntityDefinition> defs = defRepo.findByTenantIdAndArchivedFalse(tenantId);
        String schema = TenantContext.getSchema();

        List<Map<String, Object>> tables = defs.stream().map(def -> Map.<String, Object>of(
                "table", Map.of("schema", schema, "name", "entity_records"),
                "configuration", Map.of(
                        "custom_name", def.getEntityType(),
                        "column_config", Map.of(),
                        "custom_root_fields", Map.of(
                                "select", def.getEntityType() + "_list",
                                "select_by_pk", def.getEntityType() + "_by_id",
                                "insert", def.getEntityType() + "_create",
                                "update_by_pk", def.getEntityType() + "_update"
                        )
                ),
                "object_relationships", List.of(),
                "array_relationships", List.of(),
                "insert_permissions", List.of(),
                "select_permissions", List.of(Map.of(
                        "role", "platform-user",
                        "permission", Map.of(
                                "columns", List.of("id", "data", "created_at"),
                                "filter", Map.of("tenant_id", Map.of("_eq", "X-Hasura-Tenant-Id"))
                        )
                ))
        )).collect(Collectors.toList());

        return Map.of(
                "version", 3,
                "sources", List.of(Map.of(
                        "name", "platform_" + tenantId,
                        "kind", "postgres",
                        "tables", tables,
                        "configuration", Map.of(
                                "connection_info", Map.of(
                                        "database_url", Map.of("from_env", "HASURA_DB_URL_" + tenantId.toUpperCase())
                                )
                        )
                ))
        );
    }
}
