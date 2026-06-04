package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExternalAuthRegisterBO {

    @NotBlank
    private String ticket;

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    private LoginTypeEnum loginType;
}
