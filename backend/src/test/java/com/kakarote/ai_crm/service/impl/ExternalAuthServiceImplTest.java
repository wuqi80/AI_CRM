package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.config.ExternalAuthProperties;
import com.kakarote.ai_crm.entity.BO.ExternalTenantMemberRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.PO.ExternalAuthIdentity;
import com.kakarote.ai_crm.entity.PO.ExternalTenantBinding;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.ExternalTenantBindingMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.service.AuthSessionService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalAuthServiceImplTest {

    @Test
    void autoProvisionWecomIdentityShouldCreateMemberUnderExistingAuthorizedCorp() {
        ExternalAuthServiceImpl service = newService();
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");
        ManageUserService manageUserService = mapper(service, "manageUserService");
        WecomEmployeeMapper wecomEmployeeMapper = mapper(service, "wecomEmployeeMapper");

        ManagerUser createdUser = new ManagerUser();
        createdUser.setTenantId(66L);
        createdUser.setUserId(77L);
        ExternalTenantBinding binding = new ExternalTenantBinding();
        binding.setTenantId(66L);
        binding.setProvider("wecom");
        binding.setExternalTenantKey("corp_1");
        when(tenantBindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(binding);
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
                .hasMessageContaining("WeCom enterprise is not authorized");
        verify(registrationService, never()).registerExternalTenant(any(ExternalTenantRegisterBO.class));
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
        ReflectionTestUtils.setField(service, "wecomEmployeeMapper", mock(WecomEmployeeMapper.class));
        return service;
    }
}
