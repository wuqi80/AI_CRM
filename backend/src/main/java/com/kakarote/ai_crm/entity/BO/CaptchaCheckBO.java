package com.kakarote.ai_crm.entity.BO;

import com.anji.captcha.model.common.CaptchaTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "图形验证码校验请求")
public class CaptchaCheckBO {

    @NotBlank(message = "验证码 token 不能为空")
    @Schema(description = "验证码 token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @NotNull(message = "滑块横向坐标不能为空")
    @Min(value = 0, message = "滑块横向坐标不能小于 0")
    @Schema(description = "滑块横向坐标", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pointX;

    @Schema(description = "滑块纵向坐标，默认 5")
    private Integer pointY = 5;

    @Schema(description = "验证码类型，默认滑块拼图")
    private String captchaType = CaptchaTypeEnum.BLOCKPUZZLE.getCodeValue();

    @Schema(description = "验证码密钥")
    private String secretKey;
}
