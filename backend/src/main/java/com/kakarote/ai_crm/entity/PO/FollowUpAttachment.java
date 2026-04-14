package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_follow_up_attachment")
@Schema(name = "FollowUpAttachment", description = "Follow-up attachment")
public class FollowUpAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Attachment ID")
    private Long attachmentId;

    @Schema(description = "Follow-up ID")
    private Long followUpId;

    @Schema(description = "File name")
    private String fileName;

    @Schema(description = "File path")
    private String filePath;

    @Schema(description = "File size")
    private Long fileSize;

    @Schema(description = "MIME type")
    private String mimeType;

    @Schema(description = "Sort order")
    private Integer sort;

    @Schema(description = "Analysis status")
    private String analysisStatus;

    @Schema(description = "Analysis content")
    private String analysisContent;

    @Schema(description = "Analysis time")
    private Date analysisTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Create time")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "Update time")
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Tenant ID")
    private Long tenantId;
}
