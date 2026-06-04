package com.kakarote.syncdata.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MappingRepositoryTest {

    @Mock
    private JdbcTemplate target;

    @InjectMocks
    private MappingRepository repository;

    @Test
    void getOrCreateTargetIdReusesExistingMapping() {
        when(target.queryForObject(contains("INSERT INTO sync_mapping"), eq(Long.class),
                any(), eq("wk_crm"), eq("wk_crm_customer"), eq(12L), eq("34"),
                eq("crm_customer"), any(), eq(56L)))
                .thenReturn(9001L);

        long targetId = repository.getOrCreateTargetId("wk_crm_customer", 12L, 34L, "crm_customer", 56L);

        assertThat(targetId).isEqualTo(9001L);
        verify(target, never()).update(contains("INSERT INTO sync_mapping"), (Object[]) any());
    }

    @Test
    void getOrCreateTargetIdCreatesMappingWhenMissing() {
        when(target.queryForObject(contains("INSERT INTO sync_mapping"), eq(Long.class),
                any(), eq("wk_crm"), eq("wk_crm_customer"), eq(12L), eq("34"),
                eq("crm_customer"), any(), eq(56L)))
                .thenReturn(9002L);

        long targetId = repository.getOrCreateTargetId("wk_crm_customer", 12L, 34L, "crm_customer", 56L);

        assertThat(targetId).isEqualTo(9002L);
        verify(target).queryForObject(contains("RETURNING target_id"), eq(Long.class),
                any(), eq("wk_crm"), eq("wk_crm_customer"), eq(12L), eq("34"),
                eq("crm_customer"), any(), eq(56L));
    }

    @Test
    void getOrCreateTargetIdsCreatesMissingMappingsInBatch() {
        when(target.queryForList(contains("SELECT source_id, target_id"), any(Object[].class)))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        Map.of("source_id", "34", "target_id", 9001L),
                        Map.of("source_id", "35", "target_id", 9002L)
                ));

        Map<String, Long> targetIds = repository.getOrCreateTargetIds(
                "wk_crm_customer", 12L, List.of(34L, 35L), "crm_customer", 56L);

        assertThat(targetIds)
                .containsEntry("34", 9001L)
                .containsEntry("35", 9002L);
        verify(target).batchUpdate(contains("INSERT INTO sync_mapping"),
                argThat((List<Object[]> batch) -> batch.size() == 2));
    }

    @Test
    void finishJobMarksCompletedWithErrorsWhenFailuresExist() {
        repository.finishJob(1L, 10L, 8L, 2L);

        verify(target).update(contains("UPDATE sync_full_job"), eq("completed_with_errors"),
                eq(10L), eq(8L), eq(2L), eq("finished"), eq(1L));
    }

    @Test
    void finishModuleStoresStatusAndCounts() {
        repository.finishModule(1L, "customers", "wk_crm_customer", "crm_customer", 10L, 9L, 1L, "ok");

        verify(target).update(contains("INSERT INTO sync_job_module"), any(), eq(1L), eq("customers"),
                eq("completed_with_errors"), eq("wk_crm_customer"), eq("crm_customer"),
                eq(10L), eq(9L), eq(1L), eq("ok"));
    }

    @Test
    void failModuleStoresFailedStatusAndMessage() {
        repository.failModule(1L, "customers", "wk_crm_customer", "crm_customer", 10L, 6L, 0L, "batch failed");

        verify(target).update(contains("INSERT INTO sync_job_module"), any(), eq(1L), eq("customers"),
                eq("wk_crm_customer"), eq("crm_customer"), eq(10L), eq(6L), eq(0L), eq("batch failed"));
    }

    @Test
    void recordErrorPersistsRowLevelFailure() {
        repository.recordError(1L, "customers", "wk_crm_customer", 12L, "34", "bad row");

        verify(target).update(contains("INSERT INTO sync_job_error"), any(), eq(1L), eq("customers"),
                eq("wk_crm_customer"), eq(12L), eq("34"), eq("bad row"));
    }

    @Test
    void companyScopedCleanupDoesNotDeleteTenantRows() {
        repository.deleteMappedTargetRows(12L);

        verify(target, times(11)).update(contains("IN (SELECT target_id FROM sync_mapping"), any(), eq(12L));
        verify(target).update(contains("manager_role_menu"), any(), eq(12L));
        verify(target).update("DELETE FROM sync_mapping WHERE source_system = ? AND source_company_id = ?",
                "wk_crm", 12L);
        verify(target, never()).update(contains("crm_tenant"), any(), eq(12L));
    }
}
