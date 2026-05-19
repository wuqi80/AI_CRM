package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "AiBillingConfigUpdateBO", description = "AI billing config update parameters")
public class AiBillingConfigUpdateBO implements Serializable {

    @NotNull(message = "每积分 Token 数不能为空")
    @Min(value = 1, message = "每积分 Token 数必须大于 0")
    @Max(value = 1000000000, message = "每积分 Token 数不能超过 1000000000")
    @Schema(description = "Tokens covered by one credit at 1x multiplier", example = "800")
    private Integer tokensPerCredit;
}
