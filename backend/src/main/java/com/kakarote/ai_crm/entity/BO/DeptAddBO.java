package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "部门添加BO")
@Data
public class DeptAddBO {

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "上级部门ID，0为根部门")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
