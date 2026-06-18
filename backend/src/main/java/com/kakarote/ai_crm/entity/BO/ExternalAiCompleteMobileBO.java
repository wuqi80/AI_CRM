package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "外部 AI 完善手机号请求")
public class ExternalAiCompleteMobileBO {

    @Schema(description = "远端 CRM API 地址或 /external-api 根地址；留空时使用已保存配置")
    private String apiUrl;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "手机验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String verificationCode;
}
