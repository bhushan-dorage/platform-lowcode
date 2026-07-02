# ADR-0006: Redisson for Distributed Locking

**Status:** Accepted  
**Date:** 2026-01-15

## Context

The workflow engine runs as multiple horizontally scaled pods. When a user claims a human task, two concurrent requests must not both succeed — this would result in two users believing they own the same task, causing data corruption and broken SLA tracking. The locking solution must:

- Guarantee mutual exclusion across all pods for the same task ID
- Be non-blocking for other tasks — a lock on task A must not delay task B
- Time-bound — locks must auto-expire to prevent deadlocks if a pod crashes mid-claim
- Fast — locking overhead must not push task-claim p99 latency above 100ms
- Integrate cleanly with the existing Redis infrastructure already used for session caching

Candidates evaluated:

| Approach | Notes |
|---|---|
| **Redisson (Redis-based)** | Redlock algorithm, Spring-native, `RLock` API mirrors `java.util.concurrent.Lock` |
| PostgreSQL advisory locks | No extra dependency, but held for full transaction duration; can block other operations |
| Optimistic locking (JPA `@Version`) | No infrastructure needed, but requires retry loops and can starve under high contention |
| Zookeeper (Apache Curator) | Strong consistency guarantees, but adds Zookeeper as a dependency beyond Kafka's existing usage |
| Database-backed pessimistic lock (`SELECT FOR UPDATE`) | Simple, but holds a DB connection for the lock duration — connection pool exhaustion risk |

## Decision

Use **Redisson 3.x** for distributed locking in `platform-workflow-engine`.

Lock pattern in `TaskClaimService`:
- Key: `{tenantId}:task-lock:{taskId}`
- TTL: 5 seconds (auto-expire on pod crash)
- `tryLock(waitTime=0, leaseTime=5s)` — non-blocking; returns false immediately if already locked

```java
RLock lock = redissonClient.getLock(lockKey);
if (!lock.tryLock(0, 5, TimeUnit.SECONDS)) {
    throw new TaskAlreadyClaimedException(taskId);
}
try {
    // claim the task in Flowable + PostgreSQL
} finally {
    lock.unlock();
}
```

Redis is already in the stack for session caching and L2 entitlements cache — Redisson reuses the same Redis instance with no additional infrastructure.

## Consequences

**Positive:**
- Zero `TASK_ALREADY_CLAIMED` conflicts under concurrent load — validated by `TaskClaimCompleteSimulation` Gatling test
- Lock scope is per task ID — no contention between different tasks
- 5-second TTL means a crashed pod releases the lock automatically; no manual intervention required
- `tryLock(waitTime=0)` means the caller gets an immediate error, not a queue — predictable latency
- Redisson's `RLock` integrates with Spring's `@Transactional` — lock and DB write are in the same logical operation

**Negative:**
- Redis is now a hard dependency for task claiming — if Redis is unavailable, task claiming is unavailable even if Flowable and PostgreSQL are healthy
- Redlock (multi-node Redis) requires a Redis Sentinel or Cluster setup for true fault tolerance; single-node Redis in dev/staging is not Redlock-safe
- Lock TTL of 5 seconds is a trade-off: too short risks premature expiry on slow Flowable calls; too long extends deadlock window on pod crash. Current value assumes Flowable task claim completes in <2 seconds under normal load
- Redisson version must be kept compatible with the Redis server version (Redisson 3.x supports Redis 3.0+)
