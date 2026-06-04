package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingWebhookEvent;
import com.kakarote.ai_crm.mapper.TencentMeetingCorpConfigMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingWebhookEventMapper;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMeetingWebhookServiceTest {

    private static final String ENCODING_AES_KEY = "abcdefghijklmnopqrstuvwxyzABCDEFG1234567890";

    @Test
    void handleWebhookShouldSkipDuplicateTraceId() {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingWebhookEventMapper eventMapper = mock(TencentMeetingWebhookEventMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);

        TencentMeetingWebhookEvent existing = new TencentMeetingWebhookEvent();
        existing.setTraceId("trace-1");
        when(eventMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        boolean processed = service.handleWebhook("""
                {"event":"meeting.end","trace_id":"trace-1","payload":[{"meeting_info":{"meeting_id":"m-1"}}]}
                """);

        assertThat(processed).isFalse();
        verify(eventMapper, never()).insert(any(TencentMeetingWebhookEvent.class));
        verify(syncService, never()).refreshMeetingByExternalId(any(), any());
    }

    @Test
    void handleWebhookShouldVerifySignatureWhenWebhookTokenIsConfigured() {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingWebhookEventMapper eventMapper = mock(TencentMeetingWebhookEventMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "secretTextCipher", cipher);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setTenantId(99L);
        config.setWebhookTokenEncrypted(cipher.encrypt("meeting-token"));
        when(configMapper.selectWebhookConfigsIgnoreTenant()).thenReturn(List.of(config));
        when(eventMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        String body = """
                {"event":"meeting.end","trace_id":"trace-2","payload":[{"meeting_info":{"meeting_id":"m-2"}}]}
                """;
        String signature = TencentMeetingWebhookService.calculateSignature("meeting-token", "1700000000", "nonce-1", body);

        boolean processed = service.handleWebhook(body, "1700000000", "nonce-1", signature);

        assertThat(processed).isTrue();
        verify(eventMapper).insert(any(TencentMeetingWebhookEvent.class));
        verify(syncService).refreshMeetingByExternalId("meeting.end", "m-2");
    }

    @Test
    void handleWebhookShouldRejectInvalidSignatureWhenWebhookTokenIsConfigured() {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingWebhookEventMapper eventMapper = mock(TencentMeetingWebhookEventMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "secretTextCipher", cipher);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setTenantId(99L);
        config.setWebhookTokenEncrypted(cipher.encrypt("meeting-token"));
        when(configMapper.selectWebhookConfigsIgnoreTenant()).thenReturn(List.of(config));

        assertThatThrownBy(() -> service.handleWebhook(
                "{\"event\":\"meeting.end\",\"trace_id\":\"trace-3\"}",
                "1700000000",
                "nonce-1",
                "bad-signature"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature verification failed");

        verify(eventMapper, never()).insert(any(TencentMeetingWebhookEvent.class));
        verify(syncService, never()).refreshMeetingByExternalId(any(), any());
    }

    @Test
    void verifyWebhookUrlShouldDecryptCheckStrEvenWhenEncryptHeaderIsZero() throws Exception {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "secretTextCipher", cipher);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setTenantId(99L);
        config.setWebhookTokenEncrypted(cipher.encrypt("meeting-token"));
        config.setWebhookSecretEncrypted(cipher.encrypt(ENCODING_AES_KEY));
        when(configMapper.selectWebhookConfigsIgnoreTenant()).thenReturn(List.of(config));

        String checkStr = encryptTencentMeetingPayload("TencentMeeting");
        String signature = TencentMeetingWebhookService.calculateSignature(
                "meeting-token", "1700000000", "nonce-1", checkStr);

        byte[] verified = service.verifyWebhookUrl(checkStr, "1700000000", "nonce-1", signature, "0");

        assertThat(new String(verified, StandardCharsets.UTF_8)).isEqualTo("TencentMeeting");
    }

    @Test
    void handleWebhookShouldDecryptEncryptedPayloadWithEncodingAesKey() throws Exception {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingWebhookEventMapper eventMapper = mock(TencentMeetingWebhookEventMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "secretTextCipher", cipher);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setTenantId(99L);
        config.setWebhookTokenEncrypted(cipher.encrypt("meeting-token"));
        config.setWebhookSecretEncrypted(cipher.encrypt(ENCODING_AES_KEY));
        when(configMapper.selectWebhookConfigsIgnoreTenant()).thenReturn(List.of(config));
        when(eventMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        String plain = """
                {"event":"meeting.end","trace_id":"trace-encrypted","payload":[{"meeting_info":{"meeting_id":"m-encrypted"}}]}
                """;
        String encryptedData = encryptTencentMeetingPayload(plain);
        String body = "{\"data\":\"" + encryptedData + "\"}";
        String signature = TencentMeetingWebhookService.calculateSignature(
                "meeting-token", "1700000000", "nonce-1", encryptedData);

        boolean processed = service.handleWebhook(body, "1700000000", "nonce-1", signature);

        assertThat(processed).isTrue();
        verify(eventMapper).insert(any(TencentMeetingWebhookEvent.class));
        verify(syncService).refreshMeetingByExternalId("meeting.end", "m-encrypted");
    }

    private static String encryptTencentMeetingPayload(String plainText) throws Exception {
        byte[] key = Base64.getDecoder().decode(ENCODING_AES_KEY + "=");
        byte[] iv = new byte[16];
        System.arraycopy(key, 0, iv, 0, iv.length);
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        int padding = 32 - (plainBytes.length % 32);
        byte[] padded = new byte[plainBytes.length + padding];
        System.arraycopy(plainBytes, 0, padded, 0, plainBytes.length);
        for (int i = plainBytes.length; i < padded.length; i++) {
            padded[i] = (byte) padding;
        }
        Cipher encryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return Base64.getEncoder().encodeToString(encryptCipher.doFinal(padded));
    }
}
