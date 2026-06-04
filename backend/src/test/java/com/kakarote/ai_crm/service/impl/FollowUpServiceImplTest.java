package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.VO.FollowUpAiParseVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FollowUpServiceImplTest {

    private final FollowUpServiceImpl followUpService = new FollowUpServiceImpl();

    @Test
    void shouldMoveFutureFollowTimeToNextFollowTimeForPlannedContact() {
        FollowUpAiParseVO vo = new FollowUpAiParseVO();
        vo.setFollowTime("2026-04-10 10:00:00");
        vo.setNextFollowTime("");

        FollowUpAiParseVO normalized = ReflectionTestUtils.invokeMethod(
            followUpService,
            "normalizeParsedTimes",
            vo,
            "用户计划明天上午10点与河南思科科技有限公司的张总进行电话沟通，主要讨论回款情况。",
            "2026-04-09 16:20:00"
        );

        assertThat(normalized).isNotNull();
        assertThat(normalized.getFollowTime()).isEqualTo("2026-04-09 16:20:00");
        assertThat(normalized.getNextFollowTime()).isEqualTo("2026-04-10 10:00:00");
    }

    @Test
    void shouldInferNextFollowTimeFromContentWhenAiLeavesItBlank() {
        FollowUpAiParseVO vo = new FollowUpAiParseVO();
        vo.setFollowTime("2026-04-09 16:20:00");
        vo.setNextFollowTime("");

        FollowUpAiParseVO normalized = ReflectionTestUtils.invokeMethod(
            followUpService,
            "normalizeParsedTimes",
            vo,
            "已和客户确认，明天上午10点再次电话沟通回款进度。",
            "2026-04-09 16:20:00"
        );

        assertThat(normalized).isNotNull();
        assertThat(normalized.getFollowTime()).isEqualTo("2026-04-09 16:20:00");
        assertThat(normalized.getNextFollowTime()).isEqualTo("2026-04-10 10:00:00");
    }
}
