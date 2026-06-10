package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class WecomJsSdkAgentConfigVO {

    private String corpId;

    private String agentId;

    private Long timestamp;

    private String nonceStr;

    private String signature;
}
