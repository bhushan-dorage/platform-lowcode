package com.platform.workflow.process;

import com.platform.common.kafka.TenantAwareKafkaProducer;
import com.platform.common.tenant.TenantContext;
import com.platform.common.tenant.TenantTier;
import com.platform.workflow.exception.ResourceNotFoundException;
import com.platform.workflow.process.dto.ProcessStartRequest;
import com.platform.workflow.process.dto.ProcessStartResponse;
import com.platform.workflow.process.dto.ProcessStatusResponse;
import com.platform.workflow.process.messaging.ProcessStartEvent;
import org.flowable.engine.HistoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

    @Mock private TenantAwareKafkaProducer kafkaProducer;
    @Mock private ProcessTracker processTracker;
    @Mock private HistoryService historyService;
    @Mock private ClaimCheckService claimCheckService;

    @InjectMocks private ProcessService processService;

    @BeforeEach
    void setTenantContext() {
        TenantContext.set("acme", TenantTier.ENTERPRISE);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void startProcess_queuesEventAndReturnsTrackingId() {
        ProcessStartRequest req = new ProcessStartRequest("LoanApproval", "LOAN-001", Map.of(), "user1");

        ProcessStartResponse response = processService.startProcess(req);

        assertThat(response.trackingId()).isNotBlank();
        assertThat(response.status()).isEqualTo("QUEUED");
        assertThat(response.queuedAt()).isNotNull();

        verify(processTracker).record(eq("acme"), eq(response.trackingId()), any(ProcessStartEvent.class));
        verify(kafkaProducer).send(eq("process.events"), eq(response.trackingId()), any(ProcessStartEvent.class));
    }

    @Test
    void startProcess_publishedEventContainsCorrectTenantId() {
        ProcessStartRequest req = new ProcessStartRequest("OnboardEmployee", null, Map.of("dept", "Eng"), "hr1");

        processService.startProcess(req);

        ArgumentCaptor<ProcessStartEvent> captor = ArgumentCaptor.forClass(ProcessStartEvent.class);
        verify(kafkaProducer).send(eq("process.events"), anyString(), captor.capture());

        ProcessStartEvent event = captor.getValue();
        assertThat(event.tenantId()).isEqualTo("acme");
        assertThat(event.processKey()).isEqualTo("OnboardEmployee");
        assertThat(event.tier()).isEqualTo("ENTERPRISE");
    }

    @Test
    void getStatus_returnsStatusFromTracker() {
        ProcessStatusResponse expected = new ProcessStatusResponse(
                "tid1", "pid1", "STARTED", "LoanApproval", "LOAN-001",
                Instant.now(), Instant.now(), null);
        when(processTracker.getStatus("acme", "tid1")).thenReturn(expected);

        ProcessStatusResponse actual = processService.getStatus("tid1");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getStatus_throwsWhenNotFound() {
        when(processTracker.getStatus("acme", "unknown")).thenReturn(null);

        assertThatThrownBy(() -> processService.getStatus("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void startProcess_generatesDifferentTrackingIdEachCall() {
        ProcessStartRequest req = new ProcessStartRequest("LoanApproval", "KEY-1", Map.of(), "u1");

        ProcessStartResponse r1 = processService.startProcess(req);
        ProcessStartResponse r2 = processService.startProcess(req);

        assertThat(r1.trackingId()).isNotEqualTo(r2.trackingId());
    }
}
