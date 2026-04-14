package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(name = "FollowUpAttachmentVO", description = "Follow-up attachment view")
public class FollowUpAttachmentVO {

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
}
