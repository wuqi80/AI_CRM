package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WecomTokenService {

    @Autowired
    private WecomOpenPlatformService openPlatformService;

    @Autowired
    private WecomAgencyDevService agencyDevService;

    @Autowired
    private WecomOpenApiClient openApiClient;

    @Autowired
    private SecretTextCipher secretTextCipher;

    public String fetchAppAccessToken(WecomCorpConfig config) {
        return resolveCorpToken(config);
    }

    public String fetchContactAccessToken(WecomCorpConfig config) {
        return resolveCorpToken(config);
    }

    public String fetchArchiveAccessToken(WecomCorpConfig config) {
        if (config == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom archive config is not complete");
        }
        String corpId = StrUtil.blankToDefault(config.getArchiveCorpId(), config.getCorpId());
        if (StrUtil.isBlank(corpId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom archive corpId is not configured");
        }
        String archiveSecret = decryptRequired(config.getArchiveSecretEncrypted(),
                "Please configure WeCom archive Secret first");
        JSONObject token = openApiClient.fetchSelfBuiltCorpToken(corpId, archiveSecret);
        String accessToken = token == null ? null : token.getString("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom archive access token is empty");
        }
        return accessToken;
    }

    /**
     * 按授权来源取企业 access_token：代开发授权的企业走自建式 gettoken，
     * 其余（第三方应用授权）走 get_corp_token。
     */
    private String resolveCorpToken(WecomCorpConfig config) {
        if (agencyDevService.owns(config)) {
            return agencyDevService.fetchCorpAccessToken(config);
        }
        return openPlatformService.fetchCorpAccessToken(config);
    }

    private String decryptRequired(String encrypted, String message) {
        if (StrUtil.isBlank(encrypted)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, message);
        }
        String value = secretTextCipher.decrypt(encrypted);
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, message);
        }
        return value;
    }
}
