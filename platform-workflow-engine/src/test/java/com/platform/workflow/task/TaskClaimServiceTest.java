package com.platform.workflow.task;

import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskClaimServiceTest {

    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;

    @InjectMocks private TaskClaimService taskClaimService;

    @BeforeEach
    void setup() throws Exception {
        TenantContext.set("hsbc", TenantTier.ENTERPRISE);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @AfterEach
    void teardown() {
        TenantContext.clear();
    }

    @Test
    void claimWithLock_executesActionWhenLockAcquired() throws Exception {
        when(rLock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);
        AtomicBoolean executed = new AtomicBoolean(false);

        taskClaimService.claimWithLock("task-1", "user-1", () -> executed.set(true));

        assertThat(executed.get()).isTrue();
        verify(rLock).unlock();
    }

    @Test
    void claimWithLock_throwsWhenLockNotAcquired() throws Exception {
        when(rLock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> taskClaimService.claimWithLock("task-2", "user-1", () -> {}))
                .isInstanceOf(TaskAlreadyClaimedException.class)
                .hasMessageContaining("task-2");

        verify(rLock, never()).unlock();
    }

    @Test
    void claimWithLock_releasesLockEvenIfActionThrows() throws Exception {
        when(rLock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);

        assertThatThrownBy(() ->
                taskClaimService.claimWithLock("task-3", "user-1", () -> {
                    throw new RuntimeException("Flowable error");
                })
        ).isInstanceOf(RuntimeException.class);

        verify(rLock).unlock();
    }

    @Test
    void claimWithLock_usesCorrectLockKey() throws Exception {
        when(rLock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);

        taskClaimService.claimWithLock("task-42", "user-1", () -> {});

        verify(redissonClient).getLock("hsbc:task-lock:task-42");
    }
}
