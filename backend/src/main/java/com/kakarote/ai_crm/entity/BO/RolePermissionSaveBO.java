package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "角色权限配置保存BO")
@Data
public class RolePermissionSaveBO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "权限列表")
    private List<PermItem> permissions;

    @Data
    public static class PermItem {
        @Schema(description = "菜单ID")
        private Long menuId;

        @Schema(description = "数据范围: 1-本人 2-本人及下属 3-本部门 4-本部门及下属部门 5-全部")
        private Integer dataScope;
    }
}
