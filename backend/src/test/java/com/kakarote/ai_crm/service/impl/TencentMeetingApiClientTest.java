package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMeetingApiClientTest {

    @Test
    void createMeetingShouldUseOAuthHeadersOnly() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TencentMeetingApiClient client = new TencentMeetingApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "secretTextCipher", new SecretTextCipher("0123456789abcdef"));

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        TencentMeetingUserMapping account = new TencentMeetingUserMapping();
        account.setMeetingUserId("open-1");
        TencentMeetingOAuthCredential credential = new TencentMeetingOAuthCredential(config, account, "access-1");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        client.createMeeting(credential, new JSONObject());

        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        assertThat(entityCaptor.getValue().getHeaders().getFirst("AccessToken")).isEqualTo("access-1");
        assertThat(entityCaptor.getValue().getHeaders().getFirst("OpenId")).isEqualTo("open-1");
        assertThat(entityCaptor.getValue().getHeaders().containsKey("X-TC-Key")).isFalse();
        assertThat(entityCaptor.getValue().getHeaders().containsKey("X-TC-Signature")).isFalse();
    }
}
