package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.PO.CustomField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomFieldServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void initializeSystemFieldsSyncsExistingProductUnitFieldMetadata() {
        TenantContextHolder.setTenantId(1L);
        InMemoryCustomFieldService service = new InMemoryCustomFieldService(existingProductUnitTextField());

        service.initializeSystemFields("product");

        CustomField unitField = service.field("unit");
        assertThat(unitField.getFieldType()).isEqualTo("select");
        assertThat(unitField.getPlaceholder()).isEqualTo("请选择单位");
        assertThat(unitField.getOptions()).contains("\"value\":\"个\"");
        assertThat(service.updateCount).isEqualTo(1);
    }

    private static CustomField existingProductUnitTextField() {
        CustomField field = new CustomField();
        field.setFieldId(1L);
        field.setTenantId(1L);
        field.setEntityType("product");
        field.setFieldName("unit");
        field.setFieldLabel("单位");
        field.setFieldType("text");
        field.setFieldSource("system");
        field.setColumnName("unit");
        field.setColumnType("VARCHAR(50)");
        field.setPlaceholder("请输入单位");
        field.setOptions(null);
        field.setStatus(1);
        return field;
    }

    private static final class InMemoryCustomFieldService extends CustomFieldServiceImpl {
        private final List<CustomField> fields = new ArrayList<>();
        private int updateCount;

        private InMemoryCustomFieldService(CustomField... fields) {
            this.fields.addAll(List.of(fields));
        }

        @Override
        public List<CustomField> list(Wrapper<CustomField> queryWrapper) {
            return new ArrayList<>(fields);
        }

        @Override
        public boolean save(CustomField entity) {
            fields.add(entity);
            return true;
        }

        @Override
        public boolean updateById(CustomField entity) {
            updateCount++;
            return true;
        }

        private CustomField field(String fieldName) {
            return fields.stream()
                    .filter(field -> fieldName.equals(field.getFieldName()))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
