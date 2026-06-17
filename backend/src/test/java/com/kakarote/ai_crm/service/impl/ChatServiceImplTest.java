package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.app.ChatApplicationRegistry;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.SessionCreateBO;
import com.kakarote.ai_crm.entity.BO.SessionPinBO;
import com.kakarote.ai_crm.entity.PO.ChatSession;
import com.kakarote.ai_crm.mapper.ProductMapper;
import com.kakarote.ai_crm.mapper.ChatSessionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    @AfterEach
    void tearDown() {
        AiContextHolder.clear();
    }

    @Test
    void createSessionRejectsMultipleBoundObjects() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ReflectionTestUtils.setField(service, "chatApplicationRegistry", new ChatApplicationRegistry());
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        AiContextHolder.setContext(2001L, 7L);

        SessionCreateBO bo = new SessionCreateBO();
        bo.setCustomerId(11L);
        bo.setProductId(22L);

        assertThatThrownBy(() -> service.createSession(bo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能绑定一个业务对象");

        verify(chatSessionMapper, never()).insert(any(ChatSession.class));
    }

    @Test
    void createSessionRejectsUnavailableProduct() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ReflectionTestUtils.setField(service, "chatApplicationRegistry", new ChatApplicationRegistry());
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        AiContextHolder.setContext(2002L, 7L);

        SessionCreateBO bo = new SessionCreateBO();
        bo.setProductId(900L);
        when(productMapper.selectById(900L)).thenReturn(null);

        assertThatThrownBy(() -> service.createSession(bo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品不存在或无权限访问");

        verify(chatSessionMapper, never()).insert(any(ChatSession.class));
    }

    @Test
    void updateSessionPinPinsCurrentUserSessionWithoutChangingUpdateTime() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        AiContextHolder.setContext(1001L, 7L);

        ChatSession session = new ChatSession();
        session.setSessionId(1001L);
        session.setUserId(7L);
        session.setPinned(false);
        when(chatSessionMapper.selectById(1001L)).thenReturn(session);

        SessionPinBO bo = new SessionPinBO();
        bo.setPinned(true);
        service.updateSessionPin(1001L, bo);

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).updateById(sessionCaptor.capture());
        ChatSession updated = sessionCaptor.getValue();
        assertThat(updated.getPinned()).isTrue();
        assertThat(updated.getPinnedTime()).isNotNull();
        assertThat(updated.getUpdateTime()).isNull();
    }
}
