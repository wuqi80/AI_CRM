package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.ExternalAiCompleteMobileBO;
import com.kakarote.ai_crm.entity.BO.ExternalAiSmsCodeBO;
import com.kakarote.ai_crm.entity.VO.ExternalAiRegisterAndSaveVO;
import com.kakarote.ai_crm.service.ISystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExternalAiProviderRegistrationServiceTest {

    @Test
    void completeMobileShouldPersistMobileCompletedFlag() {
        RestOperations restOperations = mock(RestOperations.class);
        ISystemConfigService systemConfigService = mock(ISystemConfigService.class);
        when(systemConfigService.getConfigsByType("ai")).thenReturn(Map.of(
                "ai_provider", "wukong_external",
                "ai_api_url", "https://www.72crm.com/crmapi/external-api",
                "ai_api_key", "sk-anonymous",
                "ai_model", "qwen3.6-plus",
                "ai_temperature", "0.7",
                "ai_max_tokens", "2048"
        ));
        when(restOperations.exchange(
                eq("https://www.72crm.com/crmapi/external-api/complete-mobile"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of(
                "apiKey", "sk-mobile",
                "keyPrefix", "sk-mobile"
        )));

        ExternalAiProviderRegistrationService service =
                new ExternalAiProviderRegistrationService(restOperations, systemConfigService);
        ExternalAiCompleteMobileBO request = new ExternalAiCompleteMobileBO();
        request.setMobile("13800000000");
        request.setVerificationCode("123456");

        ExternalAiRegisterAndSaveVO result = service.completeMobile(request);

        assertThat(result.getApiKeyConfigured()).isTrue();
        verify(systemConfigService).updateManagedWukongExternalAiConfig(any(AiConfigUpdateBO.class));
        verify(systemConfigService).updateConfig("ai_wukong_external_mobile_completed", "true");
    }

    @Test
    void sendSmsCodeShouldRejectWhenMobileAlreadyCompleted() {
        RestOperations restOperations = mock(RestOperations.class);
        ISystemConfigService systemConfigService = mock(ISystemConfigService.class);
        when(systemConfigService.getConfigsByType("ai")).thenReturn(Map.of(
                "ai_wukong_external_mobile_completed", "true"
        ));
        ExternalAiProviderRegistrationService service =
                new ExternalAiProviderRegistrationService(restOperations, systemConfigService);
        ExternalAiSmsCodeBO request = new ExternalAiSmsCodeBO();
        request.setApiUrl("https://www.72crm.com/crmapi/external-api");
        request.setMobile("13800000000");
        request.setCaptchaVerification("captcha-token");

        assertThatThrownBy(() -> service.sendSmsCode(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号已完善");
        verifyNoInteractions(restOperations);
    }

    @Test
    void completeMobileShouldRejectWhenMobileAlreadyCompleted() {
        RestOperations restOperations = mock(RestOperations.class);
        ISystemConfigService systemConfigService = mock(ISystemConfigService.class);
        when(systemConfigService.getConfigsByType("ai")).thenReturn(Map.of(
                "ai_wukong_external_mobile_completed", "true"
        ));
        ExternalAiProviderRegistrationService service =
                new ExternalAiProviderRegistrationService(restOperations, systemConfigService);
        ExternalAiCompleteMobileBO request = new ExternalAiCompleteMobileBO();
        request.setMobile("13800000000");
        request.setVerificationCode("123456");

        assertThatThrownBy(() -> service.completeMobile(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号已完善");
        verifyNoInteractions(restOperations);
        verify(systemConfigService, never()).updateManagedWukongExternalAiConfig(any(AiConfigUpdateBO.class));
    }
}
