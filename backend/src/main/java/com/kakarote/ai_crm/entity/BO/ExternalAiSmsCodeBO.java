package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "外部 AI 注册短信验证码请求")
public class ExternalAiSmsCodeBO {

    @NotBlank(message = "远端服务地址不能为空")
    @Schema(description = "远端 CRM API 地址或 /external-api 根地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiUrl;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @NotBlank(message = "请先完成图形验证码校验")
    @Schema(description = "图形验证码二次校验串", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaVerification;
}
