package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "外部 AI 图形验证码代理请求")
public class ExternalAiCaptchaProxyBO {

    @NotBlank(message = "远端服务地址不能为空")
    @Schema(description = "远端 CRM API 地址或 /external-api 根地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiUrl;

    @Schema(description = "透传给远端验证码接口的请求体")
    private Map<String, Object> payload;
}
