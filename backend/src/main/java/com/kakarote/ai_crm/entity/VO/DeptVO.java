package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "部门树VO")
public class DeptVO {

    @Schema(description = "部门ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "上级部门ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "部门下用户数量")
    private Integer userCount;

    @Schema(description = "子部门列表")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<DeptVO> children;
}
