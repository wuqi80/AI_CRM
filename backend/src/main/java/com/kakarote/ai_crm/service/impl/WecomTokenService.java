package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WecomTokenService {

    @Autowired
    private WecomOpenPlatformService openPlatformService;

    public String fetchAppAccessToken(WecomCorpConfig config) {
        return openPlatformService.fetchCorpAccessToken(config);
    }

    public String fetchContactAccessToken(WecomCorpConfig config) {
        return openPlatformService.fetchCorpAccessToken(config);
    }
}
