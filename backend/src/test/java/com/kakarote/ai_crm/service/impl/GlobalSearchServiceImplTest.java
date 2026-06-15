package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;
import com.kakarote.ai_crm.service.PermissionService;
import com.kakarote.ai_crm.service.support.GlobalSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceImplTest {

    @Mock
    private GlobalSearchRepository globalSearchRepository;

    @Mock
    private PermissionService permissionService;

    private GlobalSearchServiceImpl globalSearchService;

    @BeforeEach
    void setUp() {
        globalSearchService = new GlobalSearchServiceImpl(globalSearchRepository, permissionService);
    }

    @Test
    void searchReturnsEmptyPageWhenKeywordIsBlank() {
        GlobalSearchQueryBO queryBO = new GlobalSearchQueryBO();
        queryBO.setKeyword("   ");
        queryBO.setPage(2);
        queryBO.setLimit(10);

        BasePage<GlobalSearchResultVO> result = globalSearchService.search(queryBO);

        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isZero();
        assertThat(result.getRecords()).isEmpty();
        verifyNoInteractions(globalSearchRepository, permissionService);
    }

    @Test
    void searchQueriesOnlyPermittedSources() {
        GlobalSearchQueryBO queryBO = new GlobalSearchQueryBO();
        queryBO.setKeyword("  alpha  ");
        queryBO.setPage(1);
        queryBO.setLimit(5);

        GlobalSearchResultVO customerResult = new GlobalSearchResultVO();
        customerResult.setType("customer");
        customerResult.setRecordId(1001L);
        customerResult.setTitle("Alpha Co");

        when(permissionService.hasPermission("customer:view")).thenReturn(true);
        when(permissionService.hasPermission("contact:view")).thenReturn(false);
        when(permissionService.hasPermission("task:view")).thenReturn(true);
        when(permissionService.hasPermission("schedule:view")).thenReturn(false);
        when(permissionService.hasPermission("knowledge:view")).thenReturn(false);
        when(globalSearchRepository.count("customer", "alpha", "%alpha%")).thenReturn(6L);
        when(globalSearchRepository.count("task", "alpha", "%alpha%")).thenReturn(2L);
        when(globalSearchRepository.search("customer", "alpha", "%alpha%", 5))
                .thenReturn(List.of(customerResult));
        when(globalSearchRepository.search("task", "alpha", "%alpha%", 5)).thenReturn(List.of());

        BasePage<GlobalSearchResultVO> result = globalSearchService.search(queryBO);

        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotal()).isEqualTo(8);
        assertThat(result.getRecords()).containsExactly(customerResult);
        verify(globalSearchRepository).count("customer", "alpha", "%alpha%");
        verify(globalSearchRepository).count("task", "alpha", "%alpha%");
        verify(globalSearchRepository).search("customer", "alpha", "%alpha%", 5);
        verify(globalSearchRepository).search("task", "alpha", "%alpha%", 5);
    }

    @Test
    void searchHonorsRequestedTypeWhenPermitted() {
        GlobalSearchQueryBO queryBO = new GlobalSearchQueryBO();
        queryBO.setKeyword("proposal");
        queryBO.setType("knowledge");

        when(permissionService.hasPermission("knowledge:view")).thenReturn(true);
        when(globalSearchRepository.count("knowledge", "proposal", "%proposal%")).thenReturn(0L);

        BasePage<GlobalSearchResultVO> result = globalSearchService.search(queryBO);

        assertThat(result.getRecords()).isEmpty();
        verify(globalSearchRepository).count("knowledge", "proposal", "%proposal%");
        verify(globalSearchRepository, never()).search("knowledge", "proposal", "%proposal%", 15);
    }
}
