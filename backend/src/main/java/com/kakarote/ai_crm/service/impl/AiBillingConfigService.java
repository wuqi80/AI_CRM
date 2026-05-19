package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.AiBillingConfigUpdateBO;
import com.kakarote.ai_crm.entity.PO.AiBillingConfig;
import com.kakarote.ai_crm.entity.VO.AiBillingConfigVO;
import com.kakarote.ai_crm.mapper.AiBillingConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(AiBillingConfigUpdateBO updateBO) {
        int tokensPerCredit = normalizeTokensPerCredit(
            updateBO != null ? updateBO.getTokensPerCredit() : null,
            0
        );
        if (tokensPerCredit <= 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "每积分 Token 数必须大于 0");
        }

        AiBillingConfig config = mapper.selectById(DEFAULT_CONFIG_KEY);
        if (config == null) {
            config = new AiBillingConfig();
            config.setConfigKey(DEFAULT_CONFIG_KEY);
            config.setTokensPerCredit(tokensPerCredit);
            mapper.insert(config);
            return;
        }

        config.setTokensPerCredit(tokensPerCredit);
        mapper.updateById(config);
    }

    private int normalizeTokensPerCredit(Integer tokensPerCredit, int fallback) {
        if (tokensPerCredit == null || tokensPerCredit <= 0) {
            return fallback;
        }
        return tokensPerCredit;
    }
}
