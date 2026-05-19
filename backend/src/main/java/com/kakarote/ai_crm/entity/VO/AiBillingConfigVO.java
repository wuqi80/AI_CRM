package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "AiBillingConfigVO", description = "AI billing config")
public class AiBillingConfigVO implements Serializable {

    @Schema(description = "Tokens covered by one credit at 1x multiplier")
    private Integer tokensPerCredit;
}
