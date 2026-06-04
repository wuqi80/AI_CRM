package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingOAuthAccountVO {

    private Long id;

    private String appId;

    private String sdkId;

    private String openId;

    private String userName;

    private String openCorpId;

    private String openCorpName;

    private String avatarUrl;

    private Long crmUserId;

    private String crmUserName;

    private String authStatus;

    private String scopes;

    private Date tokenExpiresAt;

    private Date lastAuthTime;

    private Date lastRefreshTime;

    private Date lastSyncTime;

    private String lastSyncError;
}
