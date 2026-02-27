package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户修改BO
 *
 * @author guomenghao
 */
@Schema(description = "用户添加BO")
@Data
public class UserAddBO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "真实姓名")
    private String realname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位")
    private String post;
}
