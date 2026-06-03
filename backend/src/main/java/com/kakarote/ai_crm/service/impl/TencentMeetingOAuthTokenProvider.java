package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.mapper.TencentMeetingUserMappingMapper;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@Slf4j
@Component
public class TencentMeetingOAuthTokenProvider {

    static final String AUTH_STATUS_ACTIVE = "ACTIVE";
    static final String AUTH_STATUS_EXPIRED = "EXPIRED";
    private static final String REFRESH_URL = "https://meeting.tencent.com/wemeet-webapi/v2/oauth2/oauth/refresh_token";
    private static final long REFRESH_SKEW_MILLIS = 5 * 60 * 1000L;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private TencentMeetingUserMappingMapper userMappingMapper;

    private final RestTemplate restTemplate;

    public TencentMeetingOAuthTokenProvider() {
        this(new RestTemplate());
    }

    TencentMeetingOAuthTokenProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TencentMeetingOAuthCredential credential(TencentMeetingCorpConfig config, TencentMeetingUserMapping account) {
        if (account == null || StrUtil.isBlank(account.getMeetingUserId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请先授权腾讯会议账号");
        }
        if (!AUTH_STATUS_ACTIVE.equals(account.getAuthStatus()) || account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "腾讯会议授权已失效，请重新授权");
        }
        if (StrUtil.isBlank(account.getAccessTokenEncrypted()) || tokenExpiresSoon(account)) {
            refresh(config, account);
        }
        return new TencentMeetingOAuthCredential(config, account, secretTextCipher.decrypt(account.getAccessTokenEncrypted()));
    }

    public void refresh(TencentMeetingCorpConfig config, TencentMeetingUserMapping account) {
        if (StrUtil.isBlank(account.getRefreshTokenEncrypted())) {
            markExpired(account, "Missing refresh token");
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "腾讯会议授权已过期，请重新授权");
        }
        JSONObject body = new JSONObject();
        body.put("sdk_id", config.getSdkId());
        body.put("open_id", account.getMeetingUserId());
        body.put("refresh_token", secretTextCipher.decrypt(account.getRefreshTokenEncrypted()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String raw = restTemplate.postForObject(REFRESH_URL, new HttpEntity<>(body.toJSONString(), headers), String.class);
            JSONObject data = extractData(raw, "Tencent Meeting OAuth refresh failed");
            saveToken(account, data);
        } catch (RestClientResponseException e) {
            markExpired(account, e.getResponseBodyAsString());
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "腾讯会议授权已过期，请重新授权");
        } catch (BusinessException e) {
            markExpired(account, e.getMessage());
            throw e;
        }
    }

    private boolean tokenExpiresSoon(TencentMeetingUserMapping account) {
        Date expiresAt = account.getTokenExpiresAt();
        return expiresAt == null || expiresAt.getTime() <= System.currentTimeMillis() + REFRESH_SKEW_MILLIS;
    }

    private void saveToken(TencentMeetingUserMapping account, JSONObject data) {
        account.setAccessTokenEncrypted(secretTextCipher.encrypt(data.getString("access_token")));
        if (StrUtil.isNotBlank(data.getString("refresh_token"))) {
            account.setRefreshTokenEncrypted(secretTextCipher.encrypt(data.getString("refresh_token")));
        }
        account.setTokenExpiresAt(resolveExpiresAt(data));
        account.setScopes(data.getJSONArray("scopes") == null ? account.getScopes() : data.getJSONArray("scopes").toJSONString());
        account.setAuthStatus(AUTH_STATUS_ACTIVE);
        account.setLastRefreshTime(new Date());
        account.setLastSyncError(null);
        userMappingMapper.updateById(account);
    }

    private void markExpired(TencentMeetingUserMapping account, String error) {
        account.setAuthStatus(AUTH_STATUS_EXPIRED);
        account.setLastSyncError(StrUtil.blankToDefault(error, "OAuth token expired"));
        userMappingMapper.updateById(account);
        log.warn("Tencent Meeting OAuth token expired: openId={}, error={}", account.getMeetingUserId(), account.getLastSyncError());
    }

    static JSONObject extractData(String raw, String errorMessage) {
        JSONObject root = JSON.parseObject(StrUtil.blankToDefault(raw, "{}"));
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            data = root;
        }
        if (StrUtil.isBlank(data.getString("access_token"))) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, errorMessage);
        }
        return data;
    }

    private Date resolveExpiresAt(JSONObject data) {
        Long epochSeconds = data.getLong("expires");
        if (epochSeconds != null) {
            return new Date(epochSeconds * 1000L);
        }
        Long expiresIn = data.getLong("expires_in");
        return expiresIn == null ? null : new Date(System.currentTimeMillis() + expiresIn * 1000L);
    }
}
