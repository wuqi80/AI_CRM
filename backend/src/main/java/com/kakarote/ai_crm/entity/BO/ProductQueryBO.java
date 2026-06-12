package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProductQueryBO", description = "产品分页查询参数")
public class ProductQueryBO extends PageEntity {

    private String keyword;
    private Long categoryId;
    private Boolean includeChildCategory;
    private String productType;
    private String status;
    private Long ownerId;

    @Schema(hidden = true)
    private Long tenantId;
}
