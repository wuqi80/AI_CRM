package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;

public record TencentMeetingOAuthCredential(
        TencentMeetingCorpConfig config,
        TencentMeetingUserMapping account,
        String accessToken
) {

    public String openId() {
        return account == null ? null : account.getMeetingUserId();
    }
}
