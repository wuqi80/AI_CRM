package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.mapper.TencentMeetingUserMappingMapper;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMeetingOAuthTokenProviderTest {

    @Test
    void credentialShouldRefreshExpiredAccessToken() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TencentMeetingOAuthTokenProvider provider = new TencentMeetingOAuthTokenProvider(restTemplate);
        TencentMeetingUserMappingMapper userMappingMapper = mock(TencentMeetingUserMappingMapper.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(provider, "secretTextCipher", cipher);
        ReflectionTestUtils.setField(provider, "userMappingMapper", userMappingMapper);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setSdkId("sdk-1");
        TencentMeetingUserMapping account = new TencentMeetingUserMapping();
        account.setMeetingUserId("open-1");
        account.setAuthStatus("ACTIVE");
        account.setStatus(1);
        account.setRefreshTokenEncrypted(cipher.encrypt("refresh-old"));
        account.setTokenExpiresAt(new Date(System.currentTimeMillis() - 1000L));

        when(restTemplate.postForObject(eq("https://meeting.tencent.com/wemeet-webapi/v2/oauth2/oauth/refresh_token"), any(), eq(String.class)))
                .thenReturn("""
                        {"data":{"access_token":"access-new","refresh_token":"refresh-new","expires":1999999999}}
                        """);

        TencentMeetingOAuthCredential credential = provider.credential(config, account);

        assertThat(credential.accessToken()).isEqualTo("access-new");
        assertThat(cipher.decrypt(account.getRefreshTokenEncrypted())).isEqualTo("refresh-new");
        assertThat(account.getAuthStatus()).isEqualTo("ACTIVE");
        verify(userMappingMapper).updateById(account);
    }
}
