# Flowable Multi-Tenancy Deep Dive

## Engine Configuration per Tier

### ENTERPRISE — Dedicated Engine
```java
// One ProcessEngine bean per enterprise tenant
@Bean("hsbc-engine")
public ProcessEngine hsbcProcessEngine(DataSource hsbcDataSource) {
    SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
    config.setDataSource(hsbcDataSource);
    config.setDatabaseSchema("hsbc_platform");
    config.setAsyncExecutorActivate(true);
    config.setAsyncExecutorCorePoolSize(16);
    config.setAsyncExecutorMaxPoolSize(64);
    config.setAsyncExecutorMaxJobsPerAcquisition(40);
    config.setAsyncExecutorLockPollRate(250);
    config.setEnableHistoryCleanup(false); // Use our archival instead
    config.setHistoryLevel(HistoryLevel.FULL);
    config.setAsyncHistoryEnabled(true);
    return config.buildProcessEngine();
}
```

### STARTER — Shared Engine with Native Tenant Isolation
```java
// Flowable native tenant_id on all tables
runtimeService.createProcessInstanceBuilder()
    .processDefinitionKey("LoanApproval")
    .tenantId(TenantContext.get())  // CRITICAL: always set tenantId
    .businessKey(businessKey)
    .start();
```

## Job Acquisition (FOR UPDATE SKIP LOCKED)
Flowable uses PostgreSQL's `FOR UPDATE SKIP LOCKED` on `act_ru_job`.
This means multiple engine nodes safely acquire jobs without ZooKeeper.
**Never add your own locking on top of this.**

## History Level Guide
- `FULL` — stores all variable values, form fields, comments. High storage.
- `AUDIT` — stores activity, task, variable names (not values). Medium.
- `ACTIVITY` — stores start/end of activities. Low storage. Starter tier.
- `NONE` — nothing. Only for testing.

## Async History Writer
For Enterprise/Professional, enable `asyncHistoryEnabled=true`.
History events are written to `act_ge_bytearray` and processed by a separate thread pool.
This decouples history writes from the critical path.

## Process Instance Migration API
Used during rollback Option B (migrate in-flight instances to previous version):
```java
ProcessInstanceMigrationBuilder builder = processMigrationService
    .createProcessInstanceMigrationBuilder()
    .migrateToProcessDefinition(previousVersionId)
    .addActivityMigrationMapping(
        ActivityMigrationMapping.createMappingFor("approvalTask_v2", "approvalTask_v1")
    );
builder.migrate(processInstanceId);
```

## Claim Check Variable Pattern
```java
// Store large payload
String refId = claimCheckService.store(tenantId, processInstanceId, "applicationData", payload);

// In Flowable variable (always small)
execution.setVariable("applicationData__ref", refId);

// Retrieve
Object payload = claimCheckService.retrieve(tenantId, processInstanceId, "applicationData");
// ClaimCheckService detects __ref suffix → fetches from Data Service
```
