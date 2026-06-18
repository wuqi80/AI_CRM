package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "外部 AI 注册保存结果")
public class ExternalAiRegisterAndSaveVO {

    @Schema(description = "保存后的 AI 服务商编码")
    private String provider;

    @Schema(description = "保存后的 API 基础地址")
    private String apiUrl;

    @Schema(description = "保存后的模型名称")
    private String model;

    @Schema(description = "远端返回的 API Key 前缀")
    private String keyPrefix;

    @Schema(description = "是否已保存 API Key")
    private Boolean apiKeyConfigured;
}
