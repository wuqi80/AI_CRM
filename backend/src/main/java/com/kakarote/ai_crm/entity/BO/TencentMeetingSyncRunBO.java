package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class TencentMeetingSyncRunBO {

    private Integer syncDays;

    private Boolean syncRecordings;

    private Boolean syncTranscripts;
}
