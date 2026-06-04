package com.kakarote.ai_crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.config.WeKnoraConfig;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.mapper.CrmTenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class WeKnoraClientTest {

    @Mock
    private CrmTenantMapper crmTenantMapper;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private WeKnoraConfig config;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        config = new WeKnoraConfig();
        config.setBaseUrl("http://weknora.test/api/v1");
        config.getInitModels().getChat().setName("qwen-max");
        config.getInitModels().getChat().setApiKey("dashscope-key");
        config.getInitModels().getEmbedding().setApiKey("dashscope-key");
    }

    @Test
    void getOrCreateTenantContext_shouldUseTenantsApiWhenGlobalApiKeyConfigured() {
        config.setApiKey("global-api-key");

        server.expect(once(), requestTo("http://weknora.test/api/v1/tenants"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "global-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "name": "tenant_1",
                          "description": "AI CRM tenant tenant_1",
                          "business": "crm"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "id": 10001,
                            "api_key": "sk-new-tenant-key"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        WeKnoraClient client = new WeKnoraClient(config, crmTenantMapper, restTemplate, new ObjectMapper());
        String apiKey = ReflectionTestUtils.invokeMethod(client, "registerWeKnoraTenant",
                "tenant_1@crm.internal", "CrmTenant!1", "tenant_1");

        assertEquals("sk-new-tenant-key", apiKey);
        server.verify();
    }

    @Test
    void getOrCreateTenantContext_shouldLoginForApiKeyWhenRegisterResponseHasNoApiKey() {
        server.expect(once(), requestTo("http://weknora.test/api/v1/auth/register"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "email": "tenant_2@crm.internal",
                          "password": "CrmTenant!2",
                          "username": "tenant_2"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "message": "Registration successful",
                          "user": {
                            "id": "user-1",
                            "tenant_id": 10002
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://weknora.test/api/v1/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "email": "tenant_2@crm.internal",
                          "password": "CrmTenant!2"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "message": "Login successful",
                          "tenant": {
                            "id": 10002,
                            "api_key": "sk-login-tenant-key"
                          },
                          "token": "jwt-token"
                        }
                        """, MediaType.APPLICATION_JSON));

        WeKnoraClient client = new WeKnoraClient(config, crmTenantMapper, restTemplate, new ObjectMapper());
        String apiKey = ReflectionTestUtils.invokeMethod(client, "registerWeKnoraTenant",
                "tenant_2@crm.internal", "CrmTenant!2", "tenant_2");

        assertEquals("sk-login-tenant-key", apiKey);
        server.verify();
    }

    @Test
    void ensureDefaultRagModels_shouldCreateChatAndEmbeddingModelsWhenMissing() {
        server.expect(once(), requestTo("http://weknora.test/api/v1/models"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "tenant-api-key"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": []
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://weknora.test/api/v1/models"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "tenant-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "name": "qwen-max",
                          "type": "KnowledgeQA",
                          "source": "remote",
                          "description": "AI CRM RAG chat model",
                          "parameters": {
                            "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                            "api_key": "dashscope-key",
                            "provider": "aliyun"
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "id": "chat-model-id"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://weknora.test/api/v1/models"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "tenant-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "name": "text-embedding-v3",
                          "type": "Embedding",
                          "source": "remote",
                          "description": "AI CRM RAG embedding model",
                          "parameters": {
                            "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                            "api_key": "dashscope-key",
                            "provider": "aliyun",
                            "embedding_parameters": {
                              "dimension": 1024
                            }
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "id": "embedding-model-id"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        WeKnoraClient client = new WeKnoraClient(config, crmTenantMapper, restTemplate, new ObjectMapper());
        Object modelContext = ReflectionTestUtils.invokeMethod(client, "ensureDefaultRagModels", "tenant-api-key");

        assertEquals("chat-model-id", ReflectionTestUtils.getField(modelContext, "summaryModelId"));
        assertEquals("embedding-model-id", ReflectionTestUtils.getField(modelContext, "embeddingModelId"));
        server.verify();
    }

    @Test
    void searchKnowledge_shouldReturnEmptyWhenAllResultsAreBelowThresholdAndDoNotMatchQueryText() {
        config.setEnabled(true);
        config.getSearch().setVectorThreshold(0.5);
        config.getSearch().setMatchCount(5);

        server.expect(once(), requestTo("http://weknora.test/api/v1/knowledge-search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "tenant-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "query": "pricing",
                          "knowledge_base_id": "kb-id"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": [
                            {
                              "knowledge_id": "insurance-doc",
                              "knowledge_title": "建筑工程一切险",
                              "content": "施工现场保险条款和理赔资料",
                              "score": 0.02
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        WeKnoraClient client = new WeKnoraClient(config, crmTenantMapper, restTemplate, new ObjectMapper());
        List<WeKnoraChunk> chunks = client.searchKnowledge("pricing", "kb-id", "tenant-api-key");

        assertThat(chunks).isEmpty();
        server.verify();
    }

    @Test
    void filterRelevantChunks_shouldKeepLowScoreReferenceOnlyWhenItMatchesQuestionText() {
        config.getSearch().setVectorThreshold(0.5);
        WeKnoraClient client = new WeKnoraClient(config, crmTenantMapper, restTemplate, new ObjectMapper());

        WeKnoraChunk faq = new WeKnoraChunk();
        faq.setKnowledgeId("faq");
        faq.setKnowledgeTitle("咨询常见问题");
        faq.setContent("开源版仅限测试和学习时免费，商业正式使用需购买授权。");
        faq.setScore(0.02);

        WeKnoraChunk unrelated = new WeKnoraChunk();
        unrelated.setKnowledgeId("insurance");
        unrelated.setKnowledgeTitle("建筑工程一切险");
        unrelated.setContent("施工现场保险条款和理赔资料。");
        unrelated.setScore(0.02);

        List<WeKnoraChunk> chunks = client.filterRelevantChunks(
                List.of(faq, unrelated),
                "开源版永久免费吗"
        );

        assertThat(chunks).containsExactly(faq);
    }
}
