package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wecom.open-platform")
public class WecomOpenPlatformProperties {

    private Boolean enabled = Boolean.FALSE;

    private String suiteId;

    private String suiteSecret;

    private String providerCorpId;

    private String providerSecret;

    private String token;

    private String encodingAesKey;

    private String callbackUrl;

    private String authRedirectUri;

    private String loginRedirectUri;

    private String frontendRedirectUri;

    private Integer authType = 0;

    private Integer stateTtlSeconds = 600;

    private Integer tokenCacheTtlBufferSeconds = 300;

    public boolean isUsable() {
        return Boolean.TRUE.equals(enabled)
                && StrUtil.isNotBlank(suiteId)
                && StrUtil.isNotBlank(suiteSecret)
                && StrUtil.isNotBlank(token)
                && StrUtil.isNotBlank(encodingAesKey);
    }

    public boolean isLoginUsable() {
        return isUsable()
                && StrUtil.isNotBlank(providerCorpId)
                && StrUtil.isNotBlank(providerSecret);
    }
}
