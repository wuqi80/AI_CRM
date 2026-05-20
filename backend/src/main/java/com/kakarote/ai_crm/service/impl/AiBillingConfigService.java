package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.AiBillingConfig;
import com.kakarote.ai_crm.entity.VO.AiBillingConfigVO;
import com.kakarote.ai_crm.mapper.AiBillingConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class AiBillingConfigService {

    public static final int DEFAULT_TOKENS_PER_CREDIT = 800;

    private static final String DEFAULT_CONFIG_KEY = "default";

    private final AiBillingConfigMapper mapper;

    public AiBillingConfigService(AiBillingConfigMapper mapper) {
        this.mapper = mapper;
    }

    public AiBillingConfigVO getConfig() {
        AiBillingConfigVO vo = new AiBillingConfigVO();
        vo.setTokensPerCredit(getTokensPerCredit());
        return vo;
    }

    public int getTokensPerCredit() {
        AiBillingConfig config = mapper.selectById(DEFAULT_CONFIG_KEY);
        Integer value = config != null ? config.getTokensPerCredit() : null;
        return normalizeTokensPerCredit(value, DEFAULT_TOKENS_PER_CREDIT);
    }

    private int normalizeTokensPerCredit(Integer tokensPerCredit, int fallback) {
        if (tokensPerCredit == null || tokensPerCredit <= 0) {
            return fallback;
        }
        return tokensPerCredit;
    }
}
