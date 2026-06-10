package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.entity.BO.WecomEmployeeSessionQueryBO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WecomControllerEndpointTest {

    @Test
    void employeeSessionEndpointShouldUseCustomerSessionViewPermissionForScrmPage() throws Exception {
        Method method = WecomController.class.getMethod("queryEmployees", WecomEmployeeSessionQueryBO.class);
        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).isEqualTo("wecomCustomerSession:view");
    }

    @Test
    void jsSdkAgentConfigEndpointShouldUseCustomerSessionViewPermission() throws Exception {
        Method method = WecomController.class.getMethod("getJsSdkAgentConfig", String.class);
        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).isEqualTo("wecomCustomerSession:view");
    }
}
