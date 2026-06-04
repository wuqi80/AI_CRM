package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class TencentMeetingOAuthStatusVO {

    private Boolean configured;

    private Boolean authorized;

    private TencentMeetingOAuthAccountVO account;
}
