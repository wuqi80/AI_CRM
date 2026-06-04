package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class ExternalTenantRegisterBO {

    private String username;

    private String email;

    private String mobile;

    private String verificationCode;

    private String password;

    private String companyName;

    private String realname;

    private Boolean emailVerificationRequired = Boolean.TRUE;
}
