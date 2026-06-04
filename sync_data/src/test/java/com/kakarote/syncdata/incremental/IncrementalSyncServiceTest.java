package com.kakarote.syncdata.incremental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.model.CompanyBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementalSyncServiceTest {

    @Mock
    private CompanyBindingRepository bindingRepository;

    @Mock
    private IncrementalEventLogRepository eventLogRepository;

    @Mock
    private EntitySyncStateRepository entityStateRepository;

    @Mock
    private CrmToAicrmEventApplier crmToAicrmEventApplier;

    private IncrementalSyncService service;

    @BeforeEach
    void setUp() {
        service = new IncrementalSyncService(
                bindingRepository,
                eventLogRepository,
                entityStateRepository,
                crmToAicrmEventApplier,
                new ObjectMapper()
        );
    }

    @Test
    void crmToAicrmEventAppliesAndUpdatesCheckpoint() {
        IncrementalSyncEvent event = customerEvent();
        CompanyBinding binding = binding(true, false);
        when(bindingRepository.findActiveByCompanyId(100L)).thenReturn(binding);
        when(crmToAicrmEventApplier.resolveTarget(event, binding))
                .thenReturn(new ApplyResult("resolved", "resolved", "crm_customer", 300L, 1L));
        when(entityStateRepository.shouldApplyCrmEvent(event, "crm_customer", 300L)).thenReturn(true);
        when(crmToAicrmEventApplier.apply(event, binding))
                .thenReturn(ApplyResult.applied("crm_customer", 300L, 1L));

        String status = service.handleEvent(event);

        assertThat(status).isEqualTo("applied");
        verify(entityStateRepository).markApplied(event, 10L, "crm_customer", 300L, 1L);
        verify(bindingRepository).updateDirectionalCheckpoint(10L, "CRM_TO_AICRM", "42");
        verify(eventLogRepository).markStatus(event, "applied", "applied");
    }

    @Test
    void olderCrmEventIsSkippedWhenAicrmAlreadyWonConflict() {
        IncrementalSyncEvent event = customerEvent();
        CompanyBinding binding = binding(true, false);
        when(bindingRepository.findActiveByCompanyId(100L)).thenReturn(binding);
        when(crmToAicrmEventApplier.resolveTarget(event, binding))
                .thenReturn(new ApplyResult("resolved", "resolved", "crm_customer", 300L, 1L));
        when(entityStateRepository.shouldApplyCrmEvent(event, "crm_customer", 300L)).thenReturn(false);

        String status = service.handleEvent(event);

        assertThat(status).isEqualTo("conflict_skipped");
        verify(crmToAicrmEventApplier, never()).apply(any(), any());
        verify(eventLogRepository).markStatus(eq(event), eq("conflict_skipped"), any());
    }

    @Test
    void disabledBindingIgnoresCrmToAicrmEvent() {
        IncrementalSyncEvent event = customerEvent();
        when(bindingRepository.findActiveByCompanyId(100L)).thenReturn(binding(false, false));

        String status = service.handleEvent(event);

        assertThat(status).isEqualTo("ignored");
        verify(crmToAicrmEventApplier, never()).apply(any(), any());
    }

    @Test
    void rejectedTagDirectionConflictIsLoggedAsFailed() {
        IncrementalSyncEvent event = customerEvent();
        when(bindingRepository.findActiveByCompanyId(100L)).thenReturn(binding(true, false));

        String status = service.rejectEvent(event, "RocketMQ tag conflicts with body direction.");

        assertThat(status).isEqualTo("failed");
        verify(eventLogRepository).markReceived(eq(event), eq(10L), any());
        verify(eventLogRepository).markStatus(event, "failed", "RocketMQ tag conflicts with body direction.");
        verify(crmToAicrmEventApplier, never()).apply(any(), any());
    }

    @Test
    void missingTypeFallsBackToSourceTableEventName() {
        IncrementalSyncEvent event = new IncrementalSyncEvent(
                SyncDirection.CRM_TO_AICRM.name(),
                null,
                "event-2",
                "trace-2",
                "crm",
                "wk_crm",
                "aicrm",
                null,
                100L,
                null,
                "wk_crm_customer",
                "200",
                null,
                SyncOperation.UPDATE.name(),
                "42",
                Instant.parse("2026-05-01T00:00:00Z"),
                Map.of("customer_id", 200L, "customer_name", "Acme"),
                null,
                "1"
        );

        assertThat(event.resolvedType()).isEqualTo("CRM_CUSTOMER_CHANGED");
    }

    private IncrementalSyncEvent customerEvent() {
        return new IncrementalSyncEvent(
                SyncDirection.CRM_TO_AICRM.name(),
                "CRM_CUSTOMER_CHANGED",
                "event-1",
                "trace-1",
                "crm",
                "wk_crm",
                "aicrm",
                null,
                100L,
                "customer",
                "wk_crm_customer",
                "200",
                null,
                SyncOperation.UPDATE.name(),
                "42",
                Instant.parse("2026-05-01T00:00:00Z"),
                Map.of("customer_id", 200L, "customer_name", "Acme"),
                null,
                "1"
        );
    }

    private CompanyBinding binding(boolean crmToAicrm, boolean aicrmToCrm) {
        return new CompanyBinding(
                10L,
                1L,
                "wk_crm",
                "wk_crm",
                100L,
                "CRM 100",
                "old_to_new",
                "completed",
                null,
                null,
                crmToAicrm,
                "legacy-topic",
                "legacy-group",
                null,
                null,
                crmToAicrm,
                aicrmToCrm,
                "crm-aicrm-sync-events",
                "sync-data-crm-to-aicrm",
                "crm-aicrm-sync-events",
                "sync-data-aicrm-to-crm",
                null,
                null,
                null,
                null,
                1,
                null,
                null,
                null
        );
    }
}
