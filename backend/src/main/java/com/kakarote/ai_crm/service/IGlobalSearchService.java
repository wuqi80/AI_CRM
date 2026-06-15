package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;

public interface IGlobalSearchService {

    BasePage<GlobalSearchResultVO> search(GlobalSearchQueryBO queryBO);
}
