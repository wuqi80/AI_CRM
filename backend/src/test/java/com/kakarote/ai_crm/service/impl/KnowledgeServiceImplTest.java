package com.kakarote.ai_crm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiAnalyzeVO;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgeServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void aiAnalyzeDocumentReturnsCachedSnapshotWithoutCallingModel() {
        KnowledgeServiceImpl service = spy(new KnowledgeServiceImpl());
        DynamicChatClientProvider chatClientProvider = mock(DynamicChatClientProvider.class);
        ReflectionTestUtils.setField(service, "chatClientProvider", chatClientProvider);

        Knowledge knowledge = buildKnowledge();
        ReflectionTestUtils.setField(knowledge, "aiAnalysisSnapshot", """
            {"coreHighlights":"Cached summary","talkingPoints":["Point A"],"relatedEntities":[{"name":"Acme","type":"customer"}]}
            """);
        doReturn(knowledge).when(service).getById(1001L);

        KnowledgeAiAnalyzeVO result = service.aiAnalyzeDocument(1001L);

        assertThat(result.getCoreHighlights()).isEqualTo("Cached summary");
        assertThat(result.getTalkingPoints()).containsExactly("Point A");
        assertThat(result.getRelatedEntities()).hasSize(1);
        assertThat(result.getRelatedEntities().getFirst().getName()).isEqualTo("Acme");
        verifyNoInteractions(chatClientProvider);
    }

    @Test
    void aiAnalyzeDocumentStoresSnapshotAfterSuccessfulAnalysis() throws Exception {
        KnowledgeServiceImpl service = spy(new KnowledgeServiceImpl());
        KnowledgeMapper knowledgeMapper = mock(KnowledgeMapper.class);
        DynamicChatClientProvider chatClientProvider = mock(DynamicChatClientProvider.class);
        ReflectionTestUtils.setField(service, "baseMapper", knowledgeMapper);
        ReflectionTestUtils.setField(service, "chatClientProvider", chatClientProvider);

        Knowledge knowledge = buildKnowledge();
        doReturn(knowledge).when(service).getById(1001L);
        mockAnalyzeResponse(chatClientProvider, """
            {"coreHighlights":"Fresh summary","talkingPoints":["Point B"],"relatedEntities":[]}
            """);

        KnowledgeAiAnalyzeVO result = service.aiAnalyzeDocument(1001L);

        assertThat(result.getCoreHighlights()).isEqualTo("Fresh summary");
        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(captor.capture());
        Knowledge updated = captor.getValue();
        assertThat(ReflectionTestUtils.getField(updated, "aiAnalysisTime")).isNotNull();
        String snapshot = (String) ReflectionTestUtils.getField(updated, "aiAnalysisSnapshot");
        JsonNode json = OBJECT_MAPPER.readTree(snapshot);
        assertThat(json.get("coreHighlights").asText()).isEqualTo("Fresh summary");
        assertThat(json.get("talkingPoints").get(0).asText()).isEqualTo("Point B");
    }

    @Test
    void aiAnalyzeDocumentDoesNotStoreFallbackWhenModelFails() {
        KnowledgeServiceImpl service = spy(new KnowledgeServiceImpl());
        KnowledgeMapper knowledgeMapper = mock(KnowledgeMapper.class);
        DynamicChatClientProvider chatClientProvider = mock(DynamicChatClientProvider.class);
        ReflectionTestUtils.setField(service, "baseMapper", knowledgeMapper);
        ReflectionTestUtils.setField(service, "chatClientProvider", chatClientProvider);

        Knowledge knowledge = buildKnowledge();
        doReturn(knowledge).when(service).getById(1001L);
        when(chatClientProvider.getChatClient()).thenThrow(new IllegalStateException("missing key"));

        KnowledgeAiAnalyzeVO result = service.aiAnalyzeDocument(1001L);

        assertThat(result.getCoreHighlights()).isEqualTo("Existing summary");
        assertThat(result.getTalkingPoints()).isEmpty();
        assertThat(result.getRelatedEntities()).isEmpty();
        verify(knowledgeMapper, never()).updateById(any(Knowledge.class));
    }

    private static Knowledge buildKnowledge() {
        Knowledge knowledge = new Knowledge();
        knowledge.setKnowledgeId(1001L);
        knowledge.setName("proposal.txt");
        knowledge.setType("document");
        knowledge.setSummary("Existing summary");
        knowledge.setContentText("Document content");
        knowledge.setUploadUserId(7L);
        return knowledge;
    }

    private static void mockAnalyzeResponse(DynamicChatClientProvider chatClientProvider, String response) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClientProvider.getChatClient()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(response);
    }
}
