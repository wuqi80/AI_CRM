package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业微信「会话内容存档 - 接收事件服务器」回调配置。
 *
 * <p>用于近实时自动同步：企微在有新存档消息时向此回调推送事件，后端据此触发增量拉取。
 * 与第三方应用回调、代开发模板回调相互独立，使用会话存档自己的一套 Token/EncodingAESKey
 * （在企业「会话内容存档」设置里配置，此处必须填同一对，URL 验证才能通过）。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wecom.archive-callback")
public class WecomArchiveCallbackProperties {

    /** 会话存档接收事件服务器回调 Token（与企业会话存档设置一致，字母数字、≤32位） */
    private String token;

    /** 会话存档接收事件服务器回调 EncodingAESKey（与企业会话存档设置一致，字母数字、固定43位） */
    private String encodingAesKey;

    /** 单次事件触发时最多连续拉取的页数（追平消息突发；每页 ≤ getchatdata 上限 1000） */
    private int eventDrainMaxPages = 10;

    public boolean isUsable() {
        return StrUtil.isNotBlank(token) && StrUtil.isNotBlank(encodingAesKey);
    }
}
