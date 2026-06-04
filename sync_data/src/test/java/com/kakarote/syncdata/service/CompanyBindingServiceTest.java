package com.kakarote.syncdata.service;

import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.model.OldCompanyOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyBindingServiceTest {

    @Mock
    private JdbcTemplate oldCrm;

    @Mock
    private CompanyBindingRepository bindingRepository;

    @Mock
    private SyncProperties properties;

    private CompanyBindingService service;

    @BeforeEach
    void setUp() {
        service = new CompanyBindingService(oldCrm, bindingRepository, properties);
        lenient().when(oldCrm.queryForObject(contains("information_schema.tables"), eq(Integer.class), any()))
                .thenAnswer(invocation -> {
                    Object tableArgument = invocation.getArgument(2);
                    String tableName = String.valueOf(tableArgument);
                    return switch (tableName) {
                        case "wk_admin_config", "wk_admin_company" -> 1;
                        default -> 0;
                    };
                });
        lenient().when(oldCrm.queryForObject(contains("information_schema.columns"), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object tableArgument = invocation.getArgument(2);
                    Object columnArgument = invocation.getArgument(3);
                    String tableName = String.valueOf(tableArgument);
                    String columnName = String.valueOf(columnArgument);
                    if ("wk_admin_config".equals(tableName)) {
                        return List.of("company_id", "name", "value").contains(columnName) ? 1 : 0;
                    }
                    if ("wk_admin_company".equals(tableName)) {
                        return List.of("company_id", "company_manage").contains(columnName) ? 1 : 0;
                    }
                    return 0;
                });
        lenient().when(oldCrm.queryForList(contains("FROM wk_admin_company\n                    WHERE company_id IS NOT NULL\n                    ORDER BY company_id")))
                .thenReturn(List.of());
    }

    @Test
    void listOldCompaniesUsesCompanyNameConfigValue() {
        when(oldCrm.queryForList(contains("FROM wk_admin_config"), eq("companyName")))
                .thenReturn(List.of(Map.of("company_id", 100L, "company_name", "源库企业")));

        List<OldCompanyOption> options = service.listOldCompanies();

        assertThat(options).hasSize(1);
        assertThat(options.getFirst().companyId()).isEqualTo(100L);
        assertThat(options.getFirst().companyName()).isEqualTo("源库企业");
    }

    @Test
    void listOldCompaniesKeepsManagerPhoneFilterAndLoadsNameByCompanyId() {
        when(oldCrm.queryForList(contains("FROM wk_admin_company"), eq("15800000000")))
                .thenReturn(List.of(Map.of("company_id", 100L, "company_manage", "15800000000")));
        when(oldCrm.queryForList(contains("FROM wk_admin_config"), any(Object[].class)))
                .thenReturn(List.of(Map.of("company_id", 100L, "company_name", "手机号绑定企业")));

        List<OldCompanyOption> options = service.listOldCompanies("15800000000");

        assertThat(options).hasSize(1);
        assertThat(options.getFirst().companyId()).isEqualTo(100L);
        assertThat(options.getFirst().companyName()).isEqualTo("手机号绑定企业");
    }
}
