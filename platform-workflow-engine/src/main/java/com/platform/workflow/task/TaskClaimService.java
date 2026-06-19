package com.platform.workflow.task;

import com.platform.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskClaimService {

    private final RedissonClient redissonClient;

    /**
     * Acquires a Redlock on the task before executing the claim action.
     * Lock key: {tenantId}:task-lock:{taskId}, TTL 5s, wait up to 5s.
     */
    public void claimWithLock(String taskId, String userId, Runnable claimAction) {
        String lockKey = TenantContext.getTenantId() + ":task-lock:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire claim lock taskId={} userId={}", taskId, userId);
                throw new TaskAlreadyClaimedException(taskId);
            }
            claimAction.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskAlreadyClaimedException(taskId);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
