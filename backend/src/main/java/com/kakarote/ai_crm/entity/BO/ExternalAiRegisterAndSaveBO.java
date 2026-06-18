package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "外部 AI 注册并保存为系统模型请求")
public class ExternalAiRegisterAndSaveBO {

    @NotBlank(message = "远端服务地址不能为空")
    @Schema(description = "远端 CRM API 地址或 /external-api 根地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiUrl;

    @Schema(description = "手机号；为空时走匿名注册")
    private String mobile;

    @Schema(description = "手机验证码；填写手机号时必填")
    private String verificationCode;

    @Schema(description = "外部 API 账户名称")
    private String accountName;

    @Schema(description = "模型名称", example = "qwen3.6-plus")
    private String model;

    @DecimalMin(value = "0.0", message = "Temperature 最小值为 0")
    @DecimalMax(value = "2.0", message = "Temperature 最大值为 2")
    @Schema(description = "Temperature 参数，范围 0-2", example = "0.7")
    private Double temperature;

    @Min(value = 100, message = "最大 Token 数最小值为 100")
    @Max(value = 128000, message = "最大 Token 数最大值为 128000")
    @Schema(description = "最大 Token 数", example = "2048")
    private Integer maxTokens;

    @Schema(description = "额外请求头 JSON")
    private String extraHeadersJson;
}
