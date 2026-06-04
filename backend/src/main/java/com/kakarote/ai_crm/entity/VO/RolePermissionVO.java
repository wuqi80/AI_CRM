package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色权限配置VO")
public class RolePermissionVO {

    @Schema(description = "模块标识")
    private String module;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "操作权限列表")
    private List<ActionPerm> actions;

    @Data
    public static class ActionPerm {
        @Schema(description = "菜单ID")
        private String menuId;

        @Schema(description = "操作标识")
        private String action;

        @Schema(description = "操作名称")
        private String actionName;

        @Schema(description = "是否启用")
        private boolean enabled;

        @Schema(description = "数据范围")
        private Integer dataScope;

        @Schema(description = "是否有数据范围选项（create操作为false）")
        private boolean hasScopeOption;
    }
}
