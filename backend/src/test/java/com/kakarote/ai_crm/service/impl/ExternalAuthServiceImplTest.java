package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.config.ExternalAuthProperties;
import com.kakarote.ai_crm.entity.BO.ExternalAuthRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantMemberRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.PO.ExternalAuthIdentity;
import com.kakarote.ai_crm.entity.PO.ExternalTenantBinding;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.ExternalAuthAuthorizeVO;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.entity.VO.WecomOpenAuthorizeVO;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.ExternalTenantBindingMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.service.AuthSessionService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalAuthServiceImplTest {

    @Test
    void createAuthorizeUrlShouldBuildWechatQrConnectUrl() {
        ExternalAuthServiceImpl service = newService();
        Redis redis = mapper(service, "redis");
        ExternalAuthProperties properties = mapper(service, "properties");
        properties.getWechat().setEnabled(Boolean.TRUE);
        properties.getWechat().setClientId("wechat_app_id");
        properties.getWechat().setClientSecret("wechat_secret");
        properties.getWechat().setRedirectUri("https://crm.example.com/crmapi/auth/external/wechat/callback");

        ExternalAuthAuthorizeVO vo = service.createAuthorizeUrl("wechat",
                "https://crm.example.com/#/login",
                mock(HttpServletRequest.class));

        assertThat(vo.getProvider()).isEqualTo("wechat");
        assertThat(vo.getAuthorizeUrl()).startsWith("https://open.weixin.qq.com/connect/qrconnect?");
        assertThat(vo.getAuthorizeUrl()).contains("appid=wechat_app_id");
        assertThat(vo.getAuthorizeUrl()).contains("redirect_uri=https://crm.example.com/crmapi/auth/external/wechat/callback");
        assertThat(vo.getAuthorizeUrl()).contains("scope=snsapi_login");
        assertThat(vo.getAuthorizeUrl()).endsWith("#wechat_redirect");
        verify(redis).setex(any(String.class), anyInt(), contains("LOGIN"));
    }

    @Test
    void fetchWechatProfileShouldResolveProfileAndExcludeOauthTokens() {
        ExternalAuthServiceImpl service = newService();
        ExternalAuthProperties.ProviderConfig config = usableWechatConfig();
        RestTemplate restTemplate = mapper(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(containsString("/sns/oauth2/access_token")))
                .andRespond(withSuccess("""
                        {
                          "access_token": "wechat_access_token",
                          "refresh_token": "wechat_refresh_token",
                          "openid": "openid_1",
                          "unionid": "union_1"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/sns/userinfo")))
                .andRespond(withSuccess("""
                        {
                          "openid": "openid_1",
                          "nickname": "Wechat User",
                          "headimgurl": "https://wx.example.com/avatar.png",
                          "unionid": "union_1"
                        }
                        """, MediaType.APPLICATION_JSON));

        ExternalAuthServiceImpl.ExternalProfile profile = ReflectionTestUtils.invokeMethod(
                service, "fetchWechatProfile", "code_1", config);

        assertThat(profile).isNotNull();
        assertThat(profile.getProvider()).isEqualTo("wechat");
        assertThat(profile.getSubject()).isEqualTo("openid_1");
        assertThat(profile.getDisplayName()).isEqualTo("Wechat User");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://wx.example.com/avatar.png");
        assertThat(profile.getExternalTenantKey()).isEqualTo("union_1");
        assertThat(profile.getRawJson()).contains("union_1");
        assertThat(profile.getRawJson()).doesNotContain("wechat_access_token", "wechat_refresh_token");
        server.verify();
    }

    @Test
    void fetchWechatProfileShouldDecodeUtf8NicknameWhenWechatOmitsCharset() {
        ExternalAuthServiceImpl service = newService();
        ExternalAuthProperties.ProviderConfig config = usableWechatConfig();
        RestTemplate restTemplate = mapper(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(containsString("/sns/oauth2/access_token")))
                .andRespond(withSuccess("""
                        {
                          "access_token": "wechat_access_token",
                          "openid": "openid_1",
                          "unionid": "union_1"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/sns/userinfo")))
                .andRespond(withSuccess("""
                        {
                          "openid": "openid_1",
                          "nickname": "测试用户",
                          "headimgurl": "https://wx.example.com/avatar.png",
                          "unionid": "union_1"
                        }
                        """.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN));

        ExternalAuthServiceImpl.ExternalProfile profile = ReflectionTestUtils.invokeMethod(
                service, "fetchWechatProfile", "code_1", config);

        assertThat(profile).isNotNull();
        assertThat(profile.getDisplayName()).isEqualTo("测试用户");
        assertThat(profile.getRawJson()).contains("测试用户");
        server.verify();
    }

    @Test
    void fetchWechatProfileShouldRejectTokenError() {
        ExternalAuthServiceImpl service = newService();
        ExternalAuthProperties.ProviderConfig config = usableWechatConfig();
        RestTemplate restTemplate = mapper(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(containsString("/sns/oauth2/access_token")))
                .andRespond(withSuccess("""
                        {
                          "errcode": 40029,
                          "errmsg": "invalid code"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "fetchWechatProfile", "bad_code", config))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("WeChat token exchange failed");
        server.verify();
    }

    @Test
    void fetchWechatProfileShouldRejectUserInfoError() {
        ExternalAuthServiceImpl service = newService();
        ExternalAuthProperties.ProviderConfig config = usableWechatConfig();
        RestTemplate restTemplate = mapper(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(containsString("/sns/oauth2/access_token")))
                .andRespond(withSuccess("""
                        {
                          "access_token": "wechat_access_token",
                          "openid": "openid_1",
                          "unionid": "union_1"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/sns/userinfo")))
                .andRespond(withSuccess("""
                        {
                          "errcode": 40003,
                          "errmsg": "invalid openid"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "fetchWechatProfile", "code_1", config))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("WeChat userinfo failed");
        server.verify();
    }

    @Test
    void autoProvisionWecomIdentityShouldCreateMemberUnderExistingAuthorizedCorp() {
        ExternalAuthServiceImpl service = newService();
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        ManageUserService manageUserService = mapper(service, "manageUserService");
        WecomEmployeeMapper wecomEmployeeMapper = mapper(service, "wecomEmployeeMapper");
        WecomOpenPlatformService wecomOpenPlatformService = mapper(service, "wecomOpenPlatformService");

        ManagerUser createdUser = new ManagerUser();
        createdUser.setTenantId(66L);
        createdUser.setUserId(77L);
        ExternalTenantBinding binding = new ExternalTenantBinding();
        binding.setTenantId(66L);
        binding.setProvider("wecom");
        binding.setExternalTenantKey("corp_1");
        when(tenantBindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(binding);
        when(wecomOpenPlatformService.isEnterpriseAuthorized("corp_1")).thenReturn(true);
        when(manageUserService.queryUsersByUsername("wecom.corp_1_user_a.corp_1@external.wecom.local"))
                .thenReturn(java.util.List.of());
        when(registrationService.registerExternalTenantMember(any())).thenReturn(createdUser);

        AtomicReference<ExternalAuthIdentity> insertedIdentity = new AtomicReference<>();
        doAnswer(invocation -> {
            ExternalAuthIdentity identity = invocation.getArgument(0);
            identity.setId(99L);
            insertedIdentity.set(identity);
            return 1;
        }).when(identityMapper).insert(any(ExternalAuthIdentity.class));
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> insertedIdentity.get());
        when(wecomEmployeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ExternalAuthServiceImpl.ExternalProfile profile = new ExternalAuthServiceImpl.ExternalProfile();
        profile.setProvider("wecom");
        profile.setSubject("corp_1:user_a");
        profile.setDisplayName("User A");
        profile.setMobile("13800000001");
        profile.setEmail("user-a@example.com");
        profile.setExternalTenantKey("corp_1");
        profile.setExternalTenantName("Corp One");
        profile.setRawJson("{}");

        ExternalAuthIdentity identity = ReflectionTestUtils.invokeMethod(service, "autoProvisionWecomIdentity", profile);

        assertThat(identity).isNotNull();
        assertThat(identity.getId()).isEqualTo(99L);
        assertThat(identity.getProvider()).isEqualTo("wecom");
        assertThat(identity.getSubject()).isEqualTo("corp_1:user_a");
        assertThat(identity.getTenantId()).isEqualTo(66L);
        assertThat(identity.getUserId()).isEqualTo(77L);
        assertThat(identity.getEmail()).isEqualTo("user-a@example.com");
        ArgumentCaptor<ExternalTenantMemberRegisterBO> registerCaptor =
                ArgumentCaptor.forClass(ExternalTenantMemberRegisterBO.class);
        verify(registrationService).registerExternalTenantMember(registerCaptor.capture());
        assertThat(registerCaptor.getValue().getMobile()).isEqualTo("13800000001");
        ArgumentCaptor<WecomEmployee> employeeCaptor = ArgumentCaptor.forClass(WecomEmployee.class);
        verify(wecomEmployeeMapper).insert(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getCorpId()).isEqualTo("corp_1");
        assertThat(employeeCaptor.getValue().getUserId()).isEqualTo("user_a");
        assertThat(employeeCaptor.getValue().getCrmUserId()).isEqualTo(77L);
        assertThat(employeeCaptor.getValue().getMobile()).isEqualTo("13800000001");
        verify(registrationService, never()).registerExternalTenant(any(ExternalTenantRegisterBO.class));
        verify(tenantBindingMapper, never()).insert(any(ExternalTenantBinding.class));
    }

    @Test
    void autoProvisionWecomIdentityShouldRejectUnknownCorpBeforeSCRMAuthorization() {
        ExternalAuthServiceImpl service = newService();
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        when(tenantBindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ExternalAuthServiceImpl.ExternalProfile profile = new ExternalAuthServiceImpl.ExternalProfile();
        profile.setProvider("wecom");
        profile.setSubject("corp_1:user_a");
        profile.setDisplayName("User A");
        profile.setExternalTenantKey("corp_1");
        profile.setExternalTenantName("Corp One");
        profile.setRawJson("{}");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "autoProvisionWecomIdentity", profile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Please authorize WeCom third-party app first");
        verify(registrationService, never()).registerExternalTenant(any(ExternalTenantRegisterBO.class));
    }

    @Test
    void handleCallbackShouldRedirectToWecomInstallWhenCorpIsNotBound() {
        ExternalAuthServiceImpl service = newService();
        Redis redis = mapper(service, "redis");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        RegistrationService registrationService = mapper(service, "registrationService");
        WecomOpenPlatformService wecomOpenPlatformService = mock(WecomOpenPlatformService.class);
        ReflectionTestUtils.setField(service, "wecomOpenPlatformService", wecomOpenPlatformService);

        ExternalAuthServiceImpl.AuthState state = new ExternalAuthServiceImpl.AuthState();
        state.setProvider("wecom");
        state.setScene("login");
        state.setRedirect("https://crm.example.com/#/login");
        when(redis.get("external-auth:state:state_1")).thenReturn(JSON.toJSONString(state));
        when(wecomOpenPlatformService.isLoginUsable()).thenReturn(true);
        when(wecomOpenPlatformService.resolveLoginCallbackUri(any(HttpServletRequest.class)))
                .thenReturn("https://crm.example.com/crmapi/auth/external/wecom/callback");
        when(wecomOpenPlatformService.fetchLoginProfile("code_1")).thenReturn(wecomProfile());
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        WecomOpenAuthorizeVO authorizeVO = new WecomOpenAuthorizeVO();
        authorizeVO.setAuthorizeUrl("https://open.work.weixin.qq.com/3rdapp/install?suite_id=suite_1&state=install_state");
        when(wecomOpenPlatformService.createDirectInstallAuthorizeUrl(eq("https://crm.example.com/#/login"),
                any(HttpServletRequest.class))).thenReturn(authorizeVO);

        String redirect = service.handleCallback("wecom", "code_1", "state_1", null, mock(HttpServletRequest.class));

        assertThat(redirect).isEqualTo("https://open.work.weixin.qq.com/3rdapp/install?suite_id=suite_1&state=install_state");
        verify(wecomOpenPlatformService).createDirectInstallAuthorizeUrl(eq("https://crm.example.com/#/login"),
                any(HttpServletRequest.class));
        verify(redis).del("external-auth:state:state_1");
        verify(registrationService, never()).registerExternalTenantMember(any());
    }

    @Test
    void handleCallbackShouldRedirectToWecomInstallWhenBindingExistsButCorpIsNotAuthorized() {
        ExternalAuthServiceImpl service = newService();
        Redis redis = mapper(service, "redis");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        RegistrationService registrationService = mapper(service, "registrationService");
        WecomOpenPlatformService wecomOpenPlatformService = mapper(service, "wecomOpenPlatformService");

        ExternalAuthServiceImpl.AuthState state = new ExternalAuthServiceImpl.AuthState();
        state.setProvider("wecom");
        state.setScene("login");
        state.setRedirect("https://crm.example.com/#/login");
        when(redis.get("external-auth:state:state_1")).thenReturn(JSON.toJSONString(state));
        when(wecomOpenPlatformService.isLoginUsable()).thenReturn(true);
        when(wecomOpenPlatformService.fetchLoginProfile("code_1")).thenReturn(wecomProfile());
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        ExternalTenantBinding staleBinding = new ExternalTenantBinding();
        staleBinding.setTenantId(66L);
        staleBinding.setProvider("wecom");
        staleBinding.setExternalTenantKey("corp_1");
        when(tenantBindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(staleBinding);
        when(wecomOpenPlatformService.isEnterpriseAuthorized("corp_1")).thenReturn(false);
        WecomOpenAuthorizeVO authorizeVO = new WecomOpenAuthorizeVO();
        authorizeVO.setAuthorizeUrl("https://open.work.weixin.qq.com/3rdapp/install?suite_id=suite_1&state=install_state");
        when(wecomOpenPlatformService.createDirectInstallAuthorizeUrl(eq("https://crm.example.com/#/login"),
                any(HttpServletRequest.class))).thenReturn(authorizeVO);

        String redirect = service.handleCallback("wecom", "code_1", "state_1", null, mock(HttpServletRequest.class));

        assertThat(redirect).isEqualTo("https://open.work.weixin.qq.com/3rdapp/install?suite_id=suite_1&state=install_state");
        verify(wecomOpenPlatformService).createDirectInstallAuthorizeUrl(eq("https://crm.example.com/#/login"),
                any(HttpServletRequest.class));
        verify(registrationService, never()).registerExternalTenantMember(any());
    }

    @Test
    void handleWorkbenchLoginShouldReturnLoginTicketForExistingWecomIdentity() {
        ExternalAuthServiceImpl service = newService();
        Redis redis = mapper(service, "redis");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        WecomOpenPlatformService wecomOpenPlatformService = mapper(service, "wecomOpenPlatformService");
        when(wecomOpenPlatformService.isLoginUsable()).thenReturn(true);
        when(wecomOpenPlatformService.fetchLoginProfile("auth_code_1")).thenReturn(wecomProfile());
        ExternalAuthIdentity identity = new ExternalAuthIdentity();
        identity.setId(99L);
        identity.setProvider("wecom");
        identity.setSubject("corp_1:user_a");
        identity.setTenantId(66L);
        identity.setUserId(77L);
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(identity);

        String redirect = service.handleWorkbenchLogin("auth_code_1",
                "https://crm.example.com/#/login",
                mock(HttpServletRequest.class));

        assertThat(redirect).startsWith("https://crm.example.com/#/login?externalLoginTicket=");
        assertThat(redirect).contains("provider=wecom");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ticketCaptor = ArgumentCaptor.forClass(String.class);
        verify(redis).setex(keyCaptor.capture(), any(Integer.class), ticketCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith("external-auth:login-ticket:");
        assertThat(ticketCaptor.getValue()).contains("\"identityId\":99");
    }

    @Test
    void registerByTicketShouldRejectUnboundWecomCorpAndRequireAuthorization() {
        ExternalAuthServiceImpl service = newService();
        Redis redis = mapper(service, "redis");
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");

        ExternalAuthServiceImpl.ExternalRegisterTicket ticket = new ExternalAuthServiceImpl.ExternalRegisterTicket();
        ticket.setProfile(wecomProfile());
        when(redis.get("external-auth:register-ticket:ticket_1")).thenReturn(JSON.toJSONString(ticket));
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(tenantBindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ExternalAuthRegisterBO bo = new ExternalAuthRegisterBO();
        bo.setTicket("ticket_1");

        assertThatThrownBy(() -> service.registerByTicket(bo, mock(HttpServletRequest.class), mock(HttpServletResponse.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Please authorize WeCom third-party app first");
        verify(redis).del("external-auth:register-ticket:ticket_1");
        verify(registrationService, never()).registerExternalTenant(any(ExternalTenantRegisterBO.class));
        verify(registrationService, never()).registerExternalTenantMember(any(ExternalTenantMemberRegisterBO.class));
    }

    @Test
    void registerByTicketShouldCreateWechatTenantWithoutEmailAndWithoutWecomSideEffects() {
        ExternalAuthServiceImpl service = newService();
        Redis redis = mapper(service, "redis");
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        WecomEmployeeMapper wecomEmployeeMapper = mapper(service, "wecomEmployeeMapper");

        ExternalAuthServiceImpl.ExternalRegisterTicket ticket = new ExternalAuthServiceImpl.ExternalRegisterTicket();
        ticket.setProfile(wechatProfile());
        when(redis.get("external-auth:register-ticket:ticket_1")).thenReturn(JSON.toJSONString(ticket));
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        ManagerUser createdUser = new ManagerUser();
        createdUser.setTenantId(66L);
        createdUser.setUserId(77L);
        when(registrationService.registerExternalTenant(any(ExternalTenantRegisterBO.class))).thenReturn(createdUser);

        ExternalAuthRegisterBO bo = new ExternalAuthRegisterBO();
        bo.setTicket("ticket_1");
        bo.setCompanyName("Wechat Corp");
        bo.setPassword("secret123");

        service.registerByTicket(bo, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        ArgumentCaptor<ExternalTenantRegisterBO> registerCaptor = ArgumentCaptor.forClass(ExternalTenantRegisterBO.class);
        verify(registrationService).registerExternalTenant(registerCaptor.capture());
        assertThat(registerCaptor.getValue().getEmail()).isNull();
        assertThat(registerCaptor.getValue().getUsername()).startsWith("wechat_");
        assertThat(registerCaptor.getValue().getUsername()).hasSize(23);
        assertThat(registerCaptor.getValue().getCompanyName()).isEqualTo("Wechat Corp");
        assertThat(registerCaptor.getValue().getPassword()).isEqualTo("secret123");
        assertThat(registerCaptor.getValue().getRealname()).isEqualTo("Wechat User");
        assertThat(registerCaptor.getValue().getEmailVerificationRequired()).isFalse();
        ArgumentCaptor<ExternalAuthIdentity> identityCaptor = ArgumentCaptor.forClass(ExternalAuthIdentity.class);
        verify(identityMapper).insert(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("wechat");
        assertThat(identityCaptor.getValue().getSubject()).isEqualTo("openid_1");
        assertThat(identityCaptor.getValue().getTenantId()).isEqualTo(66L);
        assertThat(identityCaptor.getValue().getUserId()).isEqualTo(77L);
        assertThat(identityCaptor.getValue().getEmail()).isNull();
        assertThat(identityCaptor.getValue().getExternalTenantKey()).isEqualTo("union_1");
        verify(tenantBindingMapper, never()).insert(any(ExternalTenantBinding.class));
        verify(wecomEmployeeMapper, never()).insert(any(WecomEmployee.class));
        verify(redis).del("external-auth:register-ticket:ticket_1");
    }

    @Test
    void bindIdentityShouldSaveWechatBindingForCurrentUser() {
        ExternalAuthServiceImpl service = newService();
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(identityMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ExternalAuthServiceImpl.AuthState state = new ExternalAuthServiceImpl.AuthState();
        state.setProvider("wechat");
        state.setScene("BIND");
        state.setTenantId(66L);
        state.setUserId(77L);

        ReflectionTestUtils.invokeMethod(service, "bindIdentity", state, wechatProfile());

        ArgumentCaptor<ExternalAuthIdentity> identityCaptor = ArgumentCaptor.forClass(ExternalAuthIdentity.class);
        verify(identityMapper).insert(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("wechat");
        assertThat(identityCaptor.getValue().getTenantId()).isEqualTo(66L);
        assertThat(identityCaptor.getValue().getUserId()).isEqualTo(77L);
    }

    @Test
    void bindIdentityShouldRejectWechatIdentityAlreadyBoundToAnotherUser() {
        ExternalAuthServiceImpl service = newService();
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        ExternalAuthIdentity existing = new ExternalAuthIdentity();
        existing.setProvider("wechat");
        existing.setSubject("openid_1");
        existing.setTenantId(66L);
        existing.setUserId(88L);
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        ExternalAuthServiceImpl.AuthState state = new ExternalAuthServiceImpl.AuthState();
        state.setProvider("wechat");
        state.setScene("BIND");
        state.setTenantId(66L);
        state.setUserId(77L);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "bindIdentity", state, wechatProfile()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("External identity is already bound");
        verify(identityMapper, never()).insert(any(ExternalAuthIdentity.class));
    }

    @Test
    void bindIdentityShouldRejectSecondWechatProviderForSameUser() {
        ExternalAuthServiceImpl service = newService();
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(identityMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ExternalAuthServiceImpl.AuthState state = new ExternalAuthServiceImpl.AuthState();
        state.setProvider("wechat");
        state.setScene("BIND");
        state.setTenantId(66L);
        state.setUserId(77L);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "bindIdentity", state, wechatProfile()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Please unbind this provider first");
        verify(identityMapper, never()).insert(any(ExternalAuthIdentity.class));
    }

    private static ExternalAuthServiceImpl.ExternalProfile wecomProfile() {
        ExternalAuthServiceImpl.ExternalProfile profile = new ExternalAuthServiceImpl.ExternalProfile();
        profile.setProvider("wecom");
        profile.setSubject("corp_1:user_a");
        profile.setDisplayName("User A");
        profile.setExternalTenantKey("corp_1");
        profile.setExternalTenantName("Corp One");
        profile.setRawJson("{}");
        return profile;
    }

    private static ExternalAuthServiceImpl.ExternalProfile wechatProfile() {
        ExternalAuthServiceImpl.ExternalProfile profile = new ExternalAuthServiceImpl.ExternalProfile();
        profile.setProvider("wechat");
        profile.setSubject("openid_1");
        profile.setDisplayName("Wechat User");
        profile.setAvatarUrl("https://wx.example.com/avatar.png");
        profile.setExternalTenantKey("union_1");
        profile.setEmailVerified(Boolean.FALSE);
        profile.setRawJson("{\"openid\":\"openid_1\",\"unionid\":\"union_1\"}");
        return profile;
    }

    private static ExternalAuthProperties.ProviderConfig usableWechatConfig() {
        ExternalAuthProperties.ProviderConfig config = new ExternalAuthProperties.ProviderConfig();
        config.setEnabled(Boolean.TRUE);
        config.setClientId("wechat_app_id");
        config.setClientSecret("wechat_secret");
        config.setRedirectUri("https://crm.example.com/crmapi/auth/external/wechat/callback");
        return config;
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(ExternalAuthServiceImpl service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static ExternalAuthServiceImpl newService() {
        ExternalAuthServiceImpl service = new ExternalAuthServiceImpl();
        ReflectionTestUtils.setField(service, "properties", new ExternalAuthProperties());
        ReflectionTestUtils.setField(service, "redis", mock(Redis.class));
        ReflectionTestUtils.setField(service, "identityMapper", mock(ExternalAuthIdentityMapper.class));
        ReflectionTestUtils.setField(service, "tenantBindingMapper", mock(ExternalTenantBindingMapper.class));
        ReflectionTestUtils.setField(service, "manageUserService", mock(ManageUserService.class));
        ReflectionTestUtils.setField(service, "registrationService", mock(RegistrationService.class));
        ReflectionTestUtils.setField(service, "authSessionService", mock(AuthSessionService.class));
        ReflectionTestUtils.setField(service, "wecomOpenPlatformService", mock(WecomOpenPlatformService.class));
        ReflectionTestUtils.setField(service, "wecomEmployeeMapper", mock(WecomEmployeeMapper.class));
        return service;
    }
}
