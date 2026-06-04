package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WecomTokenServiceTest {

    @Test
    void fetchAppAccessTokenShouldUseOpenPlatformTokenForThirdPartyConfig() {
        WecomTokenService service = new WecomTokenService();
        WecomOpenPlatformService openPlatformService = mock(WecomOpenPlatformService.class);
        ReflectionTestUtils.setField(service, "openPlatformService", openPlatformService);

        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        config.setPermanentCodeEncrypted(new SecretTextCipher("0123456789abcdef").encrypt("permanent-code"));
        when(openPlatformService.fetchCorpAccessToken(config)).thenReturn("corp-token");

        String token = service.fetchAppAccessToken(config);

        assertThat(token).isEqualTo("corp-token");
    }

    @Test
    void fetchContactAccessTokenShouldRejectConfigsWithoutThirdPartyAuthorization() {
        WecomTokenService service = new WecomTokenService();
        WecomOpenPlatformService openPlatformService = mock(WecomOpenPlatformService.class);
        ReflectionTestUtils.setField(service, "openPlatformService", openPlatformService);

        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        when(openPlatformService.fetchCorpAccessToken(config))
                .thenThrow(new com.kakarote.ai_crm.common.exception.BusinessException(
                        com.kakarote.ai_crm.common.result.SystemCodeEnum.SYSTEM_NO_VALID,
                        "WeCom enterprise is not authorized"));

        assertThatThrownBy(() -> service.fetchContactAccessToken(config))
                .isInstanceOf(com.kakarote.ai_crm.common.exception.BusinessException.class)
                .hasMessageContaining("WeCom enterprise is not authorized");
    }
}
