package com.kakarote.syncdata.service;

import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.db.MappingRepository;
import com.kakarote.syncdata.model.MigrationPreflightResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationPreflightServiceTest {

    @Mock
    private JdbcTemplate oldCrm;

    @Mock
    private CompanyBindingRepository bindingRepository;

    @Mock
    private MappingRepository mappingRepository;

    @Mock
    private SyncProperties properties;

    private MigrationPreflightService service;

    @BeforeEach
    void setUp() {
        service = new MigrationPreflightService(oldCrm, bindingRepository, mappingRepository, properties);
        lenient().when(properties.isTruncateBeforeSync()).thenReturn(false);
        lenient().when(mappingRepository.countMappingsForCompany(100L)).thenReturn(2L);
        lenient().when(oldCrm.queryForObject(contains("information_schema.columns"), eq(Integer.class),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        lenient().when(oldCrm.queryForObject(anyString(), eq(Long.class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.contains("wk_crm_contract")) {
                return 3L;
            }
            return 1L;
        });
    }

    @Test
    void preflightReportsReadyWithRerunAndRocketMqWarning() {
        when(oldCrm.queryForObject(contains("information_schema.tables"), eq(Integer.class),
                org.mockito.ArgumentMatchers.any())).thenReturn(1);

        MigrationPreflightResult result = service.preflight(1L, 100L, true);

        assertThat(result.ready()).isTrue();
        assertThat(result.rerun().existingMappings()).isTrue();
        assertThat(result.incremental().applicationAvailable()).isFalse();
        assertThat(result.warnings())
                .anyMatch(issue -> "rocketmq_disabled".equals(issue.code()))
                .anyMatch(issue -> "module_unavailable".equals(issue.code()) && "contracts".equals(issue.module()));
        assertThat(result.modules())
                .anyMatch(module -> "contracts".equals(module.key()) && "unavailable".equals(module.status()));
    }

    @Test
    void preflightBlocksWhenRequiredCustomerTableIsMissing() {
        when(oldCrm.queryForObject(contains("information_schema.tables"), eq(Integer.class),
                org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Object tableName = invocation.getArgument(2);
            return "wk_crm_customer".equals(tableName) ? 0 : 1;
        });

        MigrationPreflightResult result = service.preflight(1L, 100L, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.errors())
                .anyMatch(issue -> "source_table_missing".equals(issue.code()) && "customers".equals(issue.module()));
    }
}
