package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class TencentMeetingOAuthAuthorizeVO {

    private String authorizeUrl;

    private String state;
}
