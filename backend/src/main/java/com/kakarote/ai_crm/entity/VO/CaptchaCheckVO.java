package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图形验证码校验结果")
public class CaptchaCheckVO {

    @Schema(description = "图形验证码二次校验串")
    private String captchaVerification;
}
