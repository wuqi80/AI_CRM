package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.config.WecomOpenPlatformProperties;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ExternalAuthIdentity;
import com.kakarote.ai_crm.entity.PO.ExternalTenantBinding;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.entity.PO.WecomSuiteTicket;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.ExternalTenantBindingMapper;
import com.kakarote.ai_crm.mapper.WecomCorpConfigMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomSuiteTicketMapper;
import com.kakarote.ai_crm.service.RegistrationService;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WecomOpenPlatformServiceTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handleCallbackShouldStoreSuiteTicket() {
        WecomOpenPlatformService service = newService();
        WecomSuiteTicketMapper suiteTicketMapper = mapper(service, "suiteTicketMapper");
        SecretTextCipher cipher = mapper(service, "secretTextCipher");

        String body = """
                <xml>
                    <SuiteId><![CDATA[suite_1]]></SuiteId>
                    <InfoType><![CDATA[suite_ticket]]></InfoType>
                    <SuiteTicket><![CDATA[ticket_1]]></SuiteTicket>
                </xml>
                """;

        String result = service.handleCallback(body, null, null, null);

        assertThat(result).isEqualTo("success");
        ArgumentCaptor<WecomSuiteTicket> captor = ArgumentCaptor.forClass(WecomSuiteTicket.class);
        verify(suiteTicketMapper).insert(captor.capture());
        assertThat(captor.getValue().getSuiteId()).isEqualTo("suite_1");
        assertThat(cipher.decrypt(captor.getValue().getSuiteTicketEncrypted())).isEqualTo("ticket_1");
    }

    @Test
    void fetchLoginProfileShouldRejectCorpWithoutSCRMAuthorization() {
        WecomOpenPlatformService service = newService();
        Redis redis = mapper(service, "redis");
        WecomOpenApiClient apiClient = mapper(service, "apiClient");
        WecomCorpConfigMapper configMapper = mapper(service, "configMapper");
        when(redis.get("wecom:open:provider-token:ww_provider")).thenReturn("provider-token");
        JSONObject loginInfo = new JSONObject();
        loginInfo.put("corp_info", new JSONObject().fluentPut("corpid", "corp_1"));
        loginInfo.put("user_info", new JSONObject().fluentPut("userid", "user_a"));
        when(apiClient.fetchLoginInfo("provider-token", "auth_code_1")).thenReturn(loginInfo);
        when(configMapper.selectAuthorizedThirdPartyByCorpIdIgnoreTenant("corp_1")).thenReturn(null);

        assertThatThrownBy(() -> service.fetchLoginProfile("auth_code_1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("WeCom enterprise is not authorized");
    }

    @Test
    void buildLoginAuthorizeUrlShouldUseProviderCorpIdForQrSso() {
        WecomOpenPlatformService service = newService();
        WecomOpenPlatformProperties properties = mapper(service, "properties");
        properties.setProviderCorpId("ww_provider");

        String authorizeUrl = service.buildLoginAuthorizeUrl("https://crm.example.com/callback", "state_1");

        assertThat(authorizeUrl).contains("appid=ww_provider");
        assertThat(authorizeUrl).doesNotContain("appid=suite_1");
    }

    @Test
    void fetchLoginProfileShouldUseProviderLoginInfoForQrSso() {
        WecomOpenPlatformService service = newService();
        Redis redis = mapper(service, "redis");
        WecomOpenApiClient apiClient = mapper(service, "apiClient");
        WecomCorpConfigMapper configMapper = mapper(service, "configMapper");
        when(redis.get("wecom:open:provider-token:ww_provider")).thenReturn("provider-token");

        JSONObject loginInfo = new JSONObject();
        loginInfo.put("corp_info", new JSONObject().fluentPut("corpid", "corp_1"));
        loginInfo.put("user_info", new JSONObject()
                .fluentPut("userid", "user_a")
                .fluentPut("name", "Alice")
                .fluentPut("mobile", "13800000000")
                .fluentPut("email", "Alice@Example.COM")
                .fluentPut("avatar", "https://example.com/avatar.png"));
        when(apiClient.fetchLoginInfo("provider-token", "auth_code_1")).thenReturn(loginInfo);

        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        config.setCorpName("Authorized Corp");
        config.setAuthStatus(WecomOpenPlatformService.AUTH_STATUS_AUTHORIZED);
        config.setPermanentCodeEncrypted("encrypted_code");
        when(configMapper.selectAuthorizedThirdPartyByCorpIdIgnoreTenant("corp_1")).thenReturn(config);

        ExternalAuthServiceImpl.ExternalProfile profile = service.fetchLoginProfile("auth_code_1");

        assertThat(profile.getSubject()).isEqualTo("corp_1:user_a");
        assertThat(profile.getExternalTenantName()).isEqualTo("Authorized Corp");
        assertThat(profile.getDisplayName()).isEqualTo("Alice");
        assertThat(profile.getMobile()).isEqualTo("13800000000");
        assertThat(profile.getEmail()).isEqualTo("alice@example.com");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        verify(apiClient).fetchLoginInfo("provider-token", "auth_code_1");
    }

    @Test
    void handleAuthCallbackShouldRedirectWithErrorWhenStateIsMissing() {
        WecomOpenPlatformService service = newService();
        WecomOpenPlatformProperties properties = mapper(service, "properties");
        properties.setFrontendRedirectUri("https://aicrm-saas.5kcrm.cn/#/wecom/scrm");

        String redirect = service.handleAuthCallback("auth_code_1", null, null, null);

        assertThat(redirect).isEqualTo("https://aicrm-saas.5kcrm.cn/#/wecom/scrm?wecomAuth=error&message=missing_state");
    }

    @Test
    void createAuthorizeUrlShouldSetSessionInfoWithConfiguredAuthType() {
        WecomOpenPlatformService service = newService();
        WecomOpenPlatformProperties properties = mapper(service, "properties");
        properties.setAuthType(1);
        properties.setAuthRedirectUri("https://crm.example.com/wecom/open/auth/callback");
        Redis redis = mapper(service, "redis");
        WecomOpenApiClient apiClient = mapper(service, "apiClient");
        when(redis.get("wecom:open:suite-token:suite_1")).thenReturn("suite-token");
        when(apiClient.fetchPreAuthCode("suite-token"))
                .thenReturn(new JSONObject().fluentPut("pre_auth_code", "pre_auth_1"));
        mockLoginUser();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("crm.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getRequestURI()).thenReturn("/wecom/open/authorize");

        service.createAuthorizeUrl("https://crm.example.com/#/wecom/scrm", request);

        ArgumentCaptor<JSONObject> sessionInfoCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(apiClient).setSessionInfo(eq("suite-token"), eq("pre_auth_1"), sessionInfoCaptor.capture());
        assertThat(sessionInfoCaptor.getValue().getInteger("auth_type")).isEqualTo(1);
    }

    @Test
    void directInstallAuthEventShouldCreateTenantWithWecomUserIdUsernameWhenContactIsMissing() {
        WecomOpenPlatformService service = newService();
        Redis redis = mapper(service, "redis");
        WecomOpenApiClient apiClient = mapper(service, "apiClient");
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        WecomEmployeeMapper employeeMapper = mapper(service, "employeeMapper");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        when(redis.get("wecom:open:suite-token:suite_1")).thenReturn("suite-token");
        when(apiClient.fetchPermanentCode("suite-token", "auth_code_1")).thenReturn(permanentDataWithoutContact());
        when(tenantBindingMapper.selectOne(any())).thenReturn(null);
        ManagerUser createdUser = new ManagerUser();
        createdUser.setTenantId(66L);
        createdUser.setUserId(77L);
        createdUser.setUsername("user_a");
        when(registrationService.registerExternalTenant(any())).thenReturn(createdUser);
        when(employeeMapper.selectOne(any())).thenReturn(null);

        String result = service.handleCallback("""
                <xml>
                    <SuiteId><![CDATA[suite_1]]></SuiteId>
                    <InfoType><![CDATA[create_auth]]></InfoType>
                    <AuthCode><![CDATA[auth_code_1]]></AuthCode>
                </xml>
                """, null, null, null);

        assertThat(result).isEqualTo("success");
        ArgumentCaptor<ExternalTenantRegisterBO> registerCaptor = ArgumentCaptor.forClass(ExternalTenantRegisterBO.class);
        verify(registrationService).registerExternalTenant(registerCaptor.capture());
        assertThat(registerCaptor.getValue().getCompanyName()).isEqualTo("Corp One");
        assertThat(registerCaptor.getValue().getUsername()).isEqualTo("user_a");
        assertThat(registerCaptor.getValue().getEmail()).isNull();
        assertThat(registerCaptor.getValue().getPassword()).isNull();
        ArgumentCaptor<ExternalAuthIdentity> identityCaptor = ArgumentCaptor.forClass(ExternalAuthIdentity.class);
        verify(identityMapper).insert(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("wecom");
        assertThat(identityCaptor.getValue().getSubject()).isEqualTo("corp_1:user_a");
        assertThat(identityCaptor.getValue().getTenantId()).isEqualTo(66L);
        assertThat(identityCaptor.getValue().getUserId()).isEqualTo(77L);
        ArgumentCaptor<WecomEmployee> employeeCaptor = ArgumentCaptor.forClass(WecomEmployee.class);
        verify(employeeMapper).insert(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getCorpId()).isEqualTo("corp_1");
        assertThat(employeeCaptor.getValue().getUserId()).isEqualTo("user_a");
        assertThat(employeeCaptor.getValue().getCrmUserId()).isEqualTo(77L);
    }

    @Test
    void directInstallAuthEventShouldReuseExistingTenantBinding() {
        WecomOpenPlatformService service = newService();
        Redis redis = mapper(service, "redis");
        WecomOpenApiClient apiClient = mapper(service, "apiClient");
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        when(redis.get("wecom:open:suite-token:suite_1")).thenReturn("suite-token");
        when(apiClient.fetchPermanentCode("suite-token", "auth_code_1")).thenReturn(permanentDataWithoutContact());
        ExternalTenantBinding binding = new ExternalTenantBinding();
        binding.setTenantId(88L);
        binding.setProvider("wecom");
        binding.setExternalTenantKey("corp_1");
        when(tenantBindingMapper.selectOne(any())).thenReturn(binding);

        service.handleCallback("""
                <xml>
                    <SuiteId><![CDATA[suite_1]]></SuiteId>
                    <InfoType><![CDATA[change_auth]]></InfoType>
                    <AuthCode><![CDATA[auth_code_1]]></AuthCode>
                </xml>
                """, null, null, null);

        verify(registrationService, never()).registerExternalTenant(any());
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(WecomOpenPlatformService service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static JSONObject permanentDataWithoutContact() {
        JSONObject data = new JSONObject();
        data.put("auth_corpid", "corp_1");
        data.put("permanent_code", "permanent_1");
        data.put("auth_corp_info", new JSONObject()
                .fluentPut("corpid", "corp_1")
                .fluentPut("corp_name", "Corp One"));
        data.put("auth_user_info", new JSONObject()
                .fluentPut("userid", "user_a")
                .fluentPut("name", "Alice"));
        data.put("auth_info", new JSONObject()
                .fluentPut("agent", new com.alibaba.fastjson.JSONArray()
                        .fluentAdd(new JSONObject().fluentPut("agentid", "100001"))));
        return data;
    }

    private static void mockLoginUser() {
        ManagerUser user = new ManagerUser();
        user.setUserId(9L);
        user.setTenantId(99L);
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(loginUser, null));
    }

    private static WecomOpenPlatformService newService() {
        WecomOpenPlatformService service = new WecomOpenPlatformService();
        WecomOpenPlatformProperties properties = new WecomOpenPlatformProperties();
        properties.setEnabled(Boolean.TRUE);
        properties.setSuiteId("suite_1");
        properties.setSuiteSecret("suite_secret");
        properties.setProviderCorpId("ww_provider");
        properties.setProviderSecret("provider_secret");
        properties.setToken("callback_token");
        properties.setEncodingAesKey("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG");
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "apiClient", mock(WecomOpenApiClient.class));
        ReflectionTestUtils.setField(service, "cryptoService", mock(WecomCallbackCryptoService.class));
        ReflectionTestUtils.setField(service, "suiteTicketMapper", mock(WecomSuiteTicketMapper.class));
        ReflectionTestUtils.setField(service, "configMapper", mock(WecomCorpConfigMapper.class));
        ReflectionTestUtils.setField(service, "tenantBindingMapper", mock(ExternalTenantBindingMapper.class));
        ReflectionTestUtils.setField(service, "registrationService", mock(RegistrationService.class));
        ReflectionTestUtils.setField(service, "identityMapper", mock(ExternalAuthIdentityMapper.class));
        ReflectionTestUtils.setField(service, "employeeMapper", mock(WecomEmployeeMapper.class));
        ReflectionTestUtils.setField(service, "secretTextCipher", new SecretTextCipher("0123456789abcdef"));
        ReflectionTestUtils.setField(service, "redis", mock(Redis.class));
        return service;
    }
}
