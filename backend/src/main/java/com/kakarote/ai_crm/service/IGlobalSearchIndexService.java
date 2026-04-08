package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.PO.GlobalSearchIndex;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;

public interface IGlobalSearchIndexService extends IService<GlobalSearchIndex> {

    BasePage<GlobalSearchResultVO> queryPageList(GlobalSearchQueryBO queryBO);

    void refreshCustomerIndex(Long customerId);

    void refreshCustomerRelatedIndexes(Long customerId);

    void refreshContactIndex(Long contactId);

    void refreshTaskIndex(Long taskId);

    void refreshScheduleIndex(Long scheduleId);

    void refreshKnowledgeIndex(Long knowledgeId);

    void deleteByEntity(String entityType, Long entityId);

    void deleteContactIndexesByCustomerId(Long customerId);
}
