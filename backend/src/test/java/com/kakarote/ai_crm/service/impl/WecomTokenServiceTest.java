package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WecomTokenServiceTest {

    @Test
    void fetchAppAccessTokenShouldUseOpenPlatformTokenForThirdPartyConfig() {
        WecomTokenService service = new WecomTokenService();
        WecomOpenPlatformService openPlatformService = mock(WecomOpenPlatformService.class);
        WecomAgencyDevService agencyDevService = mock(WecomAgencyDevService.class);
        ReflectionTestUtils.setField(service, "openPlatformService", openPlatformService);
        ReflectionTestUtils.setField(service, "agencyDevService", agencyDevService);

        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        config.setPermanentCodeEncrypted(new SecretTextCipher("0123456789abcdef").encrypt("permanent-code"));
        when(agencyDevService.owns(config)).thenReturn(false);
        when(openPlatformService.fetchCorpAccessToken(config)).thenReturn("corp-token");

        String token = service.fetchAppAccessToken(config);

        assertThat(token).isEqualTo("corp-token");
    }

    @Test
    void fetchContactAccessTokenShouldRejectConfigsWithoutThirdPartyAuthorization() {
        WecomTokenService service = new WecomTokenService();
        WecomOpenPlatformService openPlatformService = mock(WecomOpenPlatformService.class);
        WecomAgencyDevService agencyDevService = mock(WecomAgencyDevService.class);
        ReflectionTestUtils.setField(service, "openPlatformService", openPlatformService);
        ReflectionTestUtils.setField(service, "agencyDevService", agencyDevService);

        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        when(agencyDevService.owns(config)).thenReturn(false);
        when(openPlatformService.fetchCorpAccessToken(config))
                .thenThrow(new com.kakarote.ai_crm.common.exception.BusinessException(
                        com.kakarote.ai_crm.common.result.SystemCodeEnum.SYSTEM_NO_VALID,
                        "WeCom enterprise is not authorized"));

        assertThatThrownBy(() -> service.fetchContactAccessToken(config))
                .isInstanceOf(com.kakarote.ai_crm.common.exception.BusinessException.class)
                .hasMessageContaining("WeCom enterprise is not authorized");
    }

    @Test
    void fetchContactAccessTokenShouldUseAgencyDevTokenForAgencyConfig() {
        WecomTokenService service = new WecomTokenService();
        WecomOpenPlatformService openPlatformService = mock(WecomOpenPlatformService.class);
        WecomAgencyDevService agencyDevService = mock(WecomAgencyDevService.class);
        ReflectionTestUtils.setField(service, "openPlatformService", openPlatformService);
        ReflectionTestUtils.setField(service, "agencyDevService", agencyDevService);

        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        config.setSuiteId("agency_suite");
        when(agencyDevService.owns(config)).thenReturn(true);
        when(agencyDevService.fetchCorpAccessToken(config)).thenReturn("agency-token");

        String token = service.fetchContactAccessToken(config);

        assertThat(token).isEqualTo("agency-token");
        verify(openPlatformService, never()).fetchCorpAccessToken(config);
    }

    @Test
    void fetchArchiveAccessTokenShouldUseArchiveCorpIdAndArchiveSecret() {
        WecomTokenService service = new WecomTokenService();
        WecomOpenPlatformService openPlatformService = mock(WecomOpenPlatformService.class);
        WecomAgencyDevService agencyDevService = mock(WecomAgencyDevService.class);
        WecomOpenApiClient openApiClient = mock(WecomOpenApiClient.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(service, "openPlatformService", openPlatformService);
        ReflectionTestUtils.setField(service, "agencyDevService", agencyDevService);
        ReflectionTestUtils.setField(service, "openApiClient", openApiClient);
        ReflectionTestUtils.setField(service, "secretTextCipher", cipher);

        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("encrypted_corp");
        config.setArchiveCorpId("real_corp");
        config.setArchiveSecretEncrypted(cipher.encrypt("archive-secret"));
        com.alibaba.fastjson.JSONObject tokenPayload = new com.alibaba.fastjson.JSONObject()
                .fluentPut("access_token", "archive-token")
                .fluentPut("expires_in", 7200);
        when(openApiClient.fetchSelfBuiltCorpToken("real_corp", "archive-secret")).thenReturn(tokenPayload);

        String token = service.fetchArchiveAccessToken(config);

        assertThat(token).isEqualTo("archive-token");
        verify(openApiClient).fetchSelfBuiltCorpToken("real_corp", "archive-secret");
        verifyNoInteractions(openPlatformService, agencyDevService);
    }
}
