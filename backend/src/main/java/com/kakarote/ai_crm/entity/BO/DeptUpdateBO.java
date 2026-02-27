package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "部门修改BO")
@Data
public class DeptUpdateBO {

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "上级部门ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
