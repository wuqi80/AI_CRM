package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_tencent_meeting_user_mapping")
public class TencentMeetingUserMapping implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private String appId;

    private String meetingUserId;

    private String userName;

    private String openCorpId;

    private String openCorpName;

    private String avatarUrl;

    private Long crmUserId;

    private String accessTokenEncrypted;

    private String refreshTokenEncrypted;

    private Date tokenExpiresAt;

    private String scopes;

    private String authStatus;

    private Date lastAuthTime;

    private Date lastRefreshTime;

    private Date lastSyncTime;

    private String lastSyncError;

    private Integer status;

    private Date syncedAt;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
