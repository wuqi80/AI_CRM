package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "发送邮箱验证码请求")
public class SendEmailCodeBO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotNull(message = "验证码类型不能为空")
    @Min(value = 1, message = "验证码类型不正确")
    @Max(value = 2, message = "验证码类型不正确")
    @Schema(description = "类型：1-注册，2-找回密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;

    @NotBlank(message = "请先完成图形验证码校验")
    @Schema(description = "图形验证码二次校验串", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaVerification;
}
