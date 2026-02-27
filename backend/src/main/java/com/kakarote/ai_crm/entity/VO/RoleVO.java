package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "角色VO（含用户数量）")
public class RoleVO {

    @Schema(description = "角色ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色标识符")
    private String realm;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "数据权限")
    private Integer dataType;

    @Schema(description = "用户数量")
    private Integer userCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
