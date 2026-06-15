package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.mapper.CustomFieldMapper;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CustomFieldServiceImplTest {

    @Test
    void validateUniqueFieldValueRejectsDuplicateValue() {
        CustomFieldServiceImpl service = spy(new CustomFieldServiceImpl());
        CustomFieldMapper customFieldMapper = mock(CustomFieldMapper.class);
        IDynamicSchemaService dynamicSchemaService = mock(IDynamicSchemaService.class);
        ReflectionTestUtils.setField(service, "baseMapper", customFieldMapper);
        ReflectionTestUtils.setField(service, "dynamicSchemaService", dynamicSchemaService);

        CustomFieldVO field = new CustomFieldVO();
        field.setFieldName("customerCode");
        field.setFieldLabel("客户编码");
        field.setFieldType("text");
        field.setColumnName("text_value_1");
        field.setIsUnique(true);
        doReturn(List.of(field)).when(service).getEnabledFieldsByEntity("customer");
        when(dynamicSchemaService.getTableName("customer")).thenReturn("crm_customer");
        when(dynamicSchemaService.getIdColumnName("customer")).thenReturn("customer_id");
        when(dynamicSchemaService.columnExists("crm_customer", "text_value_1")).thenReturn(true);
        when(customFieldMapper.countDuplicateCustomFieldValue(
            "crm_customer",
            "customer_id",
            1001L,
            "text_value_1",
            "ACME"
        )).thenReturn(1L);

        assertThatThrownBy(() -> service.validateUniqueFieldValue("customer", 1001L, "customerCode", "ACME"))
            .isInstanceOf(BusinessException.class);
    }
}
