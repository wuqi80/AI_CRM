package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.AiCreditRecord;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import com.kakarote.ai_crm.service.ICrmTenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @InjectMocks
    private AiQuotaService aiQuotaService;

    @Mock
    private ICrmTenantService tenantService;

    @Mock
    private AiBillingConfigService aiBillingConfigService;

    @Mock
    private AiCreditRecordService aiCreditRecordService;

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @BeforeEach
    void setUp() {
        when(aiBillingConfigService.getTokensPerCredit()).thenReturn(800);
    }

    @Test
    void systemModelConsumesCreditsAndWritesRecord() {
        Long tenantId = 1001L;
        when(tenantService.getTotalCreditRemaining(tenantId)).thenReturn(300L);
        when(tenantService.consumeCredits(tenantId, 2L)).thenReturn(
            new ICrmTenantService.CreditConsumeResult(2L, 1L, 1L, 300L, 298L));

        long creditsUsed = aiQuotaService.consumeResolvedTokens(
            tenantId,
            "chat",
            new AiQuotaService.TokenUsageSnapshot(500, 301, 801),
            BigDecimal.ONE,
            "system",
            "dashscope",
            "qwen3.6-plus",
            "chat_message",
            2002L
        );

        ArgumentCaptor<AiCreditRecord> recordCaptor = ArgumentCaptor.forClass(AiCreditRecord.class);
        verify(aiCreditRecordService).save(recordCaptor.capture());
        AiCreditRecord record = recordCaptor.getValue();
        assertThat(creditsUsed).isEqualTo(2L);
        assertThat(record.getChargeable()).isTrue();
        assertThat(record.getCreditsUsed()).isEqualTo(2L);
        assertThat(record.getGiftCreditsUsed()).isEqualTo(1L);
        assertThat(record.getPurchasedCreditsUsed()).isEqualTo(1L);
        assertThat(record.getBalanceBefore()).isEqualTo(300L);
        assertThat(record.getBalanceAfter()).isEqualTo(298L);
        assertThat(record.getReferenceType()).isEqualTo("chat_message");
        assertThat(record.getReferenceId()).isEqualTo(2002L);
    }

    @Test
    void customModelWritesZeroCreditRecordWithoutConsumingBalance() {
        Long tenantId = 1001L;
        when(tenantService.getTotalCreditRemaining(tenantId)).thenReturn(0L);

        long creditsUsed = aiQuotaService.consumeResolvedTokens(
            tenantId,
            "chat",
            new AiQuotaService.TokenUsageSnapshot(10, 10, 20),
            BigDecimal.ONE,
            "custom",
            "openai",
            "custom-model",
            "chat_message",
            2002L
        );

        ArgumentCaptor<AiCreditRecord> recordCaptor = ArgumentCaptor.forClass(AiCreditRecord.class);
        verify(aiCreditRecordService).save(recordCaptor.capture());
        verify(tenantService, never()).consumeCredits(any(), any(Long.class));
        AiCreditRecord record = recordCaptor.getValue();
        assertThat(creditsUsed).isZero();
        assertThat(record.getChargeable()).isFalse();
        assertThat(record.getCreditsUsed()).isZero();
        assertThat(record.getBalanceBefore()).isZero();
        assertThat(record.getBalanceAfter()).isZero();
    }

    @Test
    void failedBalanceConsumeDoesNotWriteRecord() {
        Long tenantId = 1001L;
        when(tenantService.getTotalCreditRemaining(tenantId)).thenReturn(1L);
        when(tenantService.consumeCredits(tenantId, 2L)).thenThrow(
            new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "AI credits are insufficient"));

        assertThatThrownBy(() -> aiQuotaService.consumeResolvedTokens(
            tenantId,
            "chat",
            new AiQuotaService.TokenUsageSnapshot(500, 301, 801),
            BigDecimal.ONE,
            "system",
            "dashscope",
            "qwen3.6-plus",
            "chat_message",
            2002L
        )).isInstanceOf(BusinessException.class);

        verify(aiCreditRecordService, never()).save(any());
    }
}
