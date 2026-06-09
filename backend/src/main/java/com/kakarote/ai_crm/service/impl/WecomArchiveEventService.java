package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.WecomArchiveCallbackProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企业微信「会话内容存档 - 接收事件服务器」事件处理：URL 验证 + 收到新消息事件时触发增量拉取。
 */
@Service
public class WecomArchiveEventService {

    private static final Logger log = LoggerFactory.getLogger(WecomArchiveEventService.class);

    @Autowired
    private WecomArchiveCallbackProperties properties;

    @Autowired
    private WecomCallbackCryptoService cryptoService;

    @Autowired
    private WecomArchiveDrainExecutor drainExecutor;

    /** GET：URL 有效性验证，返回解密后的 echostr 明文。 */
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) {
        if (!properties.isUsable()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "会话存档接收事件服务器未配置，请设置 wecom.archive-callback.token 与 wecom.archive-callback.encoding-aes-key");
        }
        return cryptoService.decrypt(properties.getToken(), properties.getEncodingAesKey(),
                msgSignature, timestamp, nonce, echoStr);
    }

    /** POST：解密事件、解析出 corpId 后异步触发该企业的增量拉取（尽力而为，不阻塞回调应答）。 */
    public void onEvent(String body, String msgSignature, String timestamp, String nonce) {
        if (!properties.isUsable()) {
            return;
        }
        try {
            String encrypted = extractTag(body, "Encrypt");
            if (StrUtil.isBlank(encrypted)) {
                return;
            }
            String xml = cryptoService.decrypt(properties.getToken(), properties.getEncodingAesKey(),
                    msgSignature, timestamp, nonce, encrypted);
            String corpId = firstNonBlank(extractTag(xml, "ToUserName"),
                    extractTag(xml, "AuthCorpId"), extractTag(xml, "CorpId"));
            if (StrUtil.isNotBlank(corpId)) {
                drainExecutor.drainByCorpId(corpId.trim(), properties.getEventDrainMaxPages());
            } else {
                log.debug("WeCom archive event without resolvable corpId, ignored");
            }
        } catch (Exception e) {
            log.warn("WeCom archive event handling failed: {}", e.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StrUtil.isNotBlank(v)) {
                return v;
            }
        }
        return null;
    }

    private static String extractTag(String xml, String tag) {
        if (StrUtil.isBlank(xml)) {
            return null;
        }
        Matcher m = Pattern.compile("<" + tag + ">\\s*(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?\\s*</" + tag + ">", Pattern.DOTALL)
                .matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }
}
