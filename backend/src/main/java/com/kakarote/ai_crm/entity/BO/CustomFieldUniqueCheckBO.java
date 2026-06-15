package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "CustomFieldUniqueCheckBO", description = "Unique field value check payload")
public class CustomFieldUniqueCheckBO {

    @NotBlank(message = "Entity type is required")
    @Schema(description = "Entity type", requiredMode = Schema.RequiredMode.REQUIRED)
    private String entityType;

    @Schema(description = "Current entity ID")
    private Long entityId;

    @NotBlank(message = "Field name is required")
    @Schema(description = "Field name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @Schema(description = "Field value")
    private Object value;
}
