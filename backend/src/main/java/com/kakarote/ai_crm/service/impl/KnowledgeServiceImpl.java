package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.KnowledgeAskBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeAiSearchBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.PO.KnowledgeTag;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiAnalyzeVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiSearchVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.entity.VO.WeKnoraKnowledge;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.mapper.KnowledgeTagMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements IKnowledgeService {

    private static final int MAX_SEARCHABLE_CONTENT_LENGTH = 20_000;
    private static final int DEFAULT_AI_SEARCH_LIMIT = 5;
    private static final int MAX_AI_SEARCH_LIMIT = 8;
    private static final int MAX_AI_SEARCH_CONTEXT_LENGTH = 1_200;

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private WeKnoraClient weKnoraClient;

    @Autowired
    private FileStorageService fileStorageService;

    @Lazy
    @Autowired
    private KnowledgeServiceImpl self;

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tika tika = new Tika();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadFile(MultipartFile file, String type, Long customerId, String summary) {
        if (customerId != null) {
            Customer customer = customerMapper.selectById(customerId);
            if (ObjectUtil.isNull(customer)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
            }
        }
        // Generate file path
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = IdUtil.fastSimpleUUID() + "." + FileUtil.extName(file.getOriginalFilename());
        String relativePath = datePath + "/" + fileName;

        // Use FileStorageService to upload file
        fileStorageService.upload(file, relativePath);

        // Create knowledge record
        Knowledge knowledge = new Knowledge();
        knowledge.setName(file.getOriginalFilename());
        knowledge.setType(StrUtil.isEmpty(type) ? "document" : type);
        knowledge.setCustomerId(customerId);
        knowledge.setFilePath(relativePath);
        knowledge.setFileSize(file.getSize());
        knowledge.setMimeType(file.getContentType());
        knowledge.setSummary(summary);
        knowledge.setContentText(extractSearchableContent(file));
        knowledge.setUploadUserId(UserUtil.getUserId());

        // 检查文件类型是否被 WeKnora 支持
        boolean weKnoraSupported = weKnoraClient.isSupportedFileType(file.getOriginalFilename());
        if (weKnoraSupported) {
            knowledge.setWeKnoraParseStatus("pending");
        } else {
            knowledge.setWeKnoraParseStatus("unsupported");
            log.info("文件类型不被 WeKnora 支持，跳过 RAG 处理: {}", file.getOriginalFilename());
        }

        save(knowledge);

        // Async upload to WeKnora (only for supported file types)
        if (weKnoraClient.isEnabled() && weKnoraSupported) {
            self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), relativePath, file.getOriginalFilename(), UserUtil.getTenantId());
        }

        globalSearchIndexService.refreshKnowledgeIndex(knowledge.getKnowledgeId());

        return knowledge.getKnowledgeId();
    }

    /**
     * 异步上传文件到 WeKnora（传入 tenantId 解决 @Async 线程上下文丢失问题）
     */
    @Async
    public void asyncUploadToWeKnora(Long knowledgeId, String relativePath, String originalFilename, Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        File tempFile = null;
        try {
            // 获取租户的 WeKnora 上下文（API Key + 知识库 ID，懒创建）
            WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(tenantId);

            // 在文件名前加上 knowledgeId 前缀，防止不同用户上传同名文件冲突
            String uniqueFilename = knowledgeId + "_" + originalFilename;
            log.info("开始上传文件到 WeKnora: knowledgeId={}, file={}, tenantKbId={}", knowledgeId, uniqueFilename, ctx.getKnowledgeBaseId());

            String localPath = fileStorageService.getLocalPath(relativePath);
            String uploadFilePath;

            if (localPath != null) {
                uploadFilePath = localPath;
            } else {
                tempFile = File.createTempFile("weknora_", "_" + originalFilename);
                try (InputStream is = fileStorageService.getFileStream(relativePath)) {
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                uploadFilePath = tempFile.getAbsolutePath();
            }

            // Upload to WeKnora using tenant-specific API Key and knowledge base
            WeKnoraKnowledge result = weKnoraClient.uploadDocument(uploadFilePath, uniqueFilename, ctx.getKnowledgeBaseId(), ctx.getApiKey());

            // Update knowledge record
            Knowledge knowledge = baseMapper.selectByIdIgnoreDataPermission(knowledgeId);
            if (knowledge != null) {
                if (result == null) {
                    baseMapper.updateParseStatusIgnoreDataPermission(knowledgeId, "failed");
                    log.error("WeKnora 上传未返回有效结果: knowledgeId={}", knowledgeId);
                    return;
                }
                baseMapper.updateWeKnoraInfoIgnoreDataPermission(
                    knowledgeId, result.getId(), result.getParseStatus()
                );
                log.info("WeKnora 上传成功: knowledgeId={}, weKnoraId={}, status={}",
                    knowledgeId, result.getId(), result.getParseStatus());

                // 轮询等待 WeKnora 解析完成
                String currentStatus = result.getParseStatus();
                if ("pending".equals(currentStatus) || "processing".equals(currentStatus)) {
                    pollWeKnoraParseStatus(knowledgeId, result.getId(), ctx.getApiKey());
                }
            }
        } catch (Exception e) {
            log.error("WeKnora 上传失败: knowledgeId={}, error={}", knowledgeId, e.getMessage(), e);
            Knowledge knowledge = baseMapper.selectByIdIgnoreDataPermission(knowledgeId);
            if (knowledge != null) {
                baseMapper.updateParseStatusIgnoreDataPermission(knowledgeId, "failed");
            }
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            TenantContextHolder.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledge(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        // Delete from WeKnora
        if (weKnoraClient.isEnabled() && StrUtil.isNotEmpty(knowledge.getWeKnoraKnowledgeId())) {
            try {
                WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(UserUtil.getTenantId());
                weKnoraClient.deleteKnowledge(knowledge.getWeKnoraKnowledgeId(), ctx.getApiKey());
                log.info("WeKnora 删除成功: weKnoraId={}", knowledge.getWeKnoraKnowledgeId());
            } catch (Exception e) {
                log.warn("WeKnora 删除失败: weKnoraId={}, error={}",
                    knowledge.getWeKnoraKnowledgeId(), e.getMessage());
            }
        }

        // Delete file from storage
        if (StrUtil.isNotEmpty(knowledge.getFilePath())) {
            fileStorageService.delete(knowledge.getFilePath());
        }

        // Delete record
        removeById(knowledgeId);

        // Delete tags
        knowledgeTagMapper.delete(
            new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getKnowledgeId, knowledgeId)
        );
        globalSearchIndexService.deleteByEntity("knowledge", knowledgeId);
    }

    @Override
    public BasePage<KnowledgeVO> queryPageList(KnowledgeQueryBO queryBO) {
        BasePage<KnowledgeVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        return page;
    }

    @Override
    public KnowledgeVO getKnowledgeDetail(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }
        return BeanUtil.copyProperties(knowledge, KnowledgeVO.class);
    }

    /**
     * 轮询 WeKnora 解析状态，直到完成或失败
     */
    @Override
    public KnowledgeAiSearchVO aiSearch(KnowledgeAiSearchBO searchBO) {
        long startAt = System.currentTimeMillis();
        String keyword = searchBO != null ? StrUtil.trim(searchBO.getKeyword()) : null;
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "搜索内容不能为空");
        }

        String type = searchBO != null ? StrUtil.trim(searchBO.getType()) : null;
        int limit = DEFAULT_AI_SEARCH_LIMIT;
        if (searchBO != null && searchBO.getLimit() != null && searchBO.getLimit() > 0) {
            limit = Math.min(searchBO.getLimit(), MAX_AI_SEARCH_LIMIT);
        }

        List<KnowledgeAiSearchVO.ReferenceItem> references = buildSemanticReferences(keyword, type, limit);
        if (references.isEmpty()) {
            references = buildLocalReferences(keyword, type, limit);
        }

        KnowledgeAiSearchVO result = new KnowledgeAiSearchVO();
        result.setKeyword(keyword);
        result.setReferences(references);
        result.setTotalHits(references.size());
        result.setMatchPercent(calculateOverallMatchPercent(references));
        result.setAnswer(buildAiSearchAnswer(keyword, references));
        result.setTookMs(System.currentTimeMillis() - startAt);
        return result;
    }

    private void pollWeKnoraParseStatus(Long knowledgeId, String weKnoraKnowledgeId, String tenantApiKey) {
        int maxAttempts = 60;
        long intervalMs = 20000; // 20秒

        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                WeKnoraKnowledge detail = weKnoraClient.getKnowledgeDetail(weKnoraKnowledgeId, tenantApiKey);
                if (detail == null) {
                    log.warn("WeKnora 状态查询返回 null: weKnoraId={}", weKnoraKnowledgeId);
                    continue;
                }

                String status = detail.getParseStatus();
                log.debug("WeKnora 解析状态轮询: knowledgeId={}, attempt={}, status={}",
                    knowledgeId, i + 1, status);

                if ("completed".equals(status) || "success".equals(status) || "failed".equals(status)) {
                    Knowledge knowledge = baseMapper.selectByIdIgnoreDataPermission(knowledgeId);
                    if (knowledge != null) {
                        baseMapper.updateParseStatusIgnoreDataPermission(
                            knowledgeId, "success".equals(status) ? "completed" : status
                        );
                        log.info("WeKnora 解析完成: knowledgeId={}, status={}", knowledgeId, status);
                    }
                    return;
                }
            } catch (Exception e) {
                log.warn("WeKnora 状态查询异常: knowledgeId={}, error={}", knowledgeId, e.getMessage());
            }
        }

        log.warn("WeKnora 解析状态轮询超时(20min): knowledgeId={}, weKnoraId={}", knowledgeId, weKnoraKnowledgeId);
    }

    @Override
    public void reparseKnowledge(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }
        if (!weKnoraClient.isEnabled()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeKnora 未启用");
        }
        if (!weKnoraClient.isSupportedFileType(knowledge.getName())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该文件类型不支持 RAG 解析");
        }

        // 删除 WeKnora 中的旧记录（如果存在）
        if (StrUtil.isNotEmpty(knowledge.getWeKnoraKnowledgeId())) {
            try {
                WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(UserUtil.getTenantId());
                weKnoraClient.deleteKnowledge(knowledge.getWeKnoraKnowledgeId(), ctx.getApiKey());
                log.info("WeKnora 旧记录已删除: weKnoraId={}", knowledge.getWeKnoraKnowledgeId());
            } catch (Exception e) {
                log.warn("删除 WeKnora 旧记录失败: {}", e.getMessage());
            }
        }

        // 重置状态
        knowledge.setWeKnoraParseStatus("pending");
        knowledge.setWeKnoraKnowledgeId(null);
        knowledge.setContentText(extractSearchableContent(knowledge.getFilePath(), knowledge.getMimeType(), knowledge.getName()));
        updateById(knowledge);
        globalSearchIndexService.refreshKnowledgeIndex(knowledgeId);

        // 重新异步上传（含轮询）
self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), knowledge.getFilePath(), knowledge.getName(), UserUtil.getTenantId());
    }

    @Override
    public void addTag(Long knowledgeId, String tagName) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        // Check if tag already exists
        Long count = knowledgeTagMapper.selectCount(
            new LambdaQueryWrapper<KnowledgeTag>()
                .eq(KnowledgeTag::getKnowledgeId, knowledgeId)
                .eq(KnowledgeTag::getTagName, tagName)
        );
        if (count > 0) {
            return;
        }

        KnowledgeTag tag = new KnowledgeTag();
        tag.setKnowledgeId(knowledgeId);
        tag.setTagName(tagName);
        knowledgeTagMapper.insert(tag);
        globalSearchIndexService.refreshKnowledgeIndex(knowledgeId);
    }

    // ==================== AI 文档分析 ====================

    private static final String AI_ANALYZE_PROMPT_TEMPLATE = """
        你是一个专业的 CRM 助手。请分析以下知识库文档，提取关键信息并以 JSON 格式返回。

        文档名称: %s
        文档类型: %s
        文档摘要: %s

        文档内容:
        %s

        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "coreHighlights": "文档核心内容的精炼总结（2-3句话，突出关键信息和价值点）",
          "talkingPoints": ["基于文档内容的销售话术建议1", "话术建议2", "话术建议3"],
          "relatedEntities": [{"name": "相关的客户或商机名称", "type": "customer 或 opportunity"}]
        }

        注意：
        - coreHighlights 应该是对文档最重要内容的高度概括
        - talkingPoints 应该是3-5条可以在销售场景中使用的话术建议
        - relatedEntities 从文档中提取提到的客户名称、公司名称或商机/项目名称
        """;

    private static final String DOC_QA_SYSTEM_PROMPT_TEMPLATE = """
        你是一个专业的文档问答助手。请基于以下文档内容回答用户的问题。

        文档名称: %s
        文档类型: %s

        文档内容:
        %s

        请用中文回答，回答要准确、简洁。如果文档中没有相关信息，请如实说明。
        """;

    @Override
    public KnowledgeAiAnalyzeVO aiAnalyzeDocument(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        String contentText = StrUtil.blankToDefault(knowledge.getContentText(), "");
        // 截断过长的内容
        if (contentText.length() > 4000) {
            contentText = contentText.substring(0, 4000) + "...";
        }

        String summary = StrUtil.blankToDefault(knowledge.getSummary(), "无");
        String prompt = String.format(AI_ANALYZE_PROMPT_TEMPLATE,
                knowledge.getName(),
                StrUtil.blankToDefault(knowledge.getType(), "document"),
                summary,
                contentText);

        try {
            String response = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("AI 文档分析原始响应: {}", response);
            return parseAnalyzeResponse(response, knowledge);
        } catch (Exception e) {
            log.error("AI 文档分析失败，返回默认值", e);
            return buildFallbackAnalyzeResult(knowledge);
        }
    }

    private KnowledgeAiAnalyzeVO parseAnalyzeResponse(String response, Knowledge knowledge) {
        try {
            // Strip markdown code block markers if present
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            KnowledgeAiAnalyzeVO vo = new KnowledgeAiAnalyzeVO();

            // 核心提炼
            if (root.has("coreHighlights") && !root.get("coreHighlights").isNull()) {
                vo.setCoreHighlights(root.get("coreHighlights").asText());
            } else {
                vo.setCoreHighlights(StrUtil.blankToDefault(knowledge.getSummary(), "暂无摘要"));
            }

            // 推荐话术
            List<String> talkingPoints = new ArrayList<>();
            if (root.has("talkingPoints") && root.get("talkingPoints").isArray()) {
                root.get("talkingPoints").forEach(n -> talkingPoints.add(n.asText()));
            }
            vo.setTalkingPoints(talkingPoints);

            // 关联实体
            List<KnowledgeAiAnalyzeVO.RelatedEntity> entities = new ArrayList<>();
            if (root.has("relatedEntities") && root.get("relatedEntities").isArray()) {
                root.get("relatedEntities").forEach(n -> {
                    KnowledgeAiAnalyzeVO.RelatedEntity entity = new KnowledgeAiAnalyzeVO.RelatedEntity();
                    entity.setName(n.has("name") ? n.get("name").asText() : "");
                    entity.setType(n.has("type") ? n.get("type").asText() : "");
                    if (StrUtil.isNotBlank(entity.getName())) {
                        entities.add(entity);
                    }
                });
            }
            vo.setRelatedEntities(entities);

            return vo;
        } catch (Exception e) {
            log.warn("AI 文档分析 JSON 解析失败: {}", e.getMessage());
            return buildFallbackAnalyzeResult(knowledge);
        }
    }

    private KnowledgeAiAnalyzeVO buildFallbackAnalyzeResult(Knowledge knowledge) {
        KnowledgeAiAnalyzeVO vo = new KnowledgeAiAnalyzeVO();
        vo.setCoreHighlights(StrUtil.blankToDefault(knowledge.getSummary(), "暂无摘要"));
        vo.setTalkingPoints(List.of());
        vo.setRelatedEntities(List.of());
        return vo;
    }

    @Override
    public Flux<String> askDocumentQuestion(Long knowledgeId, KnowledgeAskBO askBO) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        // 设置 AI 上下文
        Long currentUserId = UserUtil.getUserIdOrNull();
        Long currentTenantId = UserUtil.getTenantId();
        if (currentUserId != null) {
            AiContextHolder.setContext(knowledgeId, currentUserId, currentTenantId);
        }

        // 构建文档内容上下文
        String contentText = StrUtil.blankToDefault(knowledge.getContentText(), "");
        if (contentText.length() > 4000) {
            contentText = contentText.substring(0, 4000) + "...";
        }

        String systemPrompt = String.format(DOC_QA_SYSTEM_PROMPT_TEMPLATE,
                knowledge.getName(),
                StrUtil.blankToDefault(knowledge.getType(), "document"),
                contentText);

        // 构建对话历史
        List<Message> history = new ArrayList<>();
        if (askBO.getHistory() != null) {
            for (KnowledgeAskBO.ChatHistoryItem item : askBO.getHistory()) {
                if ("user".equals(item.getRole())) {
                    history.add(new UserMessage(item.getContent()));
                } else if ("assistant".equals(item.getRole())) {
                    history.add(new AssistantMessage(item.getContent()));
                }
            }
        }

        // 流式调用
        return chatClientProvider.getChatClient()
                .prompt()
                .system(systemPrompt)
                .messages(history)
                .user(askBO.getQuestion())
                .stream()
                .chatResponse()
                .mapNotNull(chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        return chatResponse.getResult().getOutput().getText();
                    }
                    return null;
                })
                .doOnComplete(AiContextHolder::clear)
                .doOnError(error -> {
                    log.error("文档问答错误: {}", error.getMessage(), error);
                    AiContextHolder.clear();
                });
    }

    private List<KnowledgeAiSearchVO.ReferenceItem> buildSemanticReferences(String keyword, String type, int limit) {
        if (!weKnoraClient.isEnabled()) {
            return List.of();
        }

        Long tenantId = UserUtil.getTenantId();
        if (tenantId == null) {
            return List.of();
        }

        try {
            WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(tenantId);
            List<WeKnoraChunk> chunks = weKnoraClient.searchKnowledge(keyword, ctx.getKnowledgeBaseId(), ctx.getApiKey());
            if (chunks.isEmpty()) {
                return List.of();
            }

            Set<String> weKnoraIds = new LinkedHashSet<>();
            for (WeKnoraChunk chunk : chunks) {
                if (StrUtil.isNotBlank(chunk.getKnowledgeId())) {
                    weKnoraIds.add(chunk.getKnowledgeId());
                }
            }
            if (weKnoraIds.isEmpty()) {
                return List.of();
            }

            LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<Knowledge>()
                .in(Knowledge::getWeKnoraKnowledgeId, weKnoraIds)
                .ne(Knowledge::getStatus, 2);
            if (StrUtil.isNotBlank(type)) {
                wrapper.eq(Knowledge::getType, type);
            }

            List<Knowledge> localKnowledges = list(wrapper);
            if (localKnowledges.isEmpty()) {
                return List.of();
            }

            Map<String, Knowledge> knowledgeByWeKnoraId = new LinkedHashMap<>();
            for (Knowledge knowledge : localKnowledges) {
                if (StrUtil.isNotBlank(knowledge.getWeKnoraKnowledgeId())) {
                    knowledgeByWeKnoraId.put(knowledge.getWeKnoraKnowledgeId(), knowledge);
                }
            }

            Map<Long, String> customerNameMap = loadCustomerNameMap(localKnowledges);
            LinkedHashMap<Long, KnowledgeAiSearchVO.ReferenceItem> deduped = new LinkedHashMap<>();
            for (WeKnoraChunk chunk : chunks) {
                Knowledge knowledge = knowledgeByWeKnoraId.get(chunk.getKnowledgeId());
                if (knowledge == null) {
                    continue;
                }

                KnowledgeAiSearchVO.ReferenceItem item = deduped.computeIfAbsent(
                    knowledge.getKnowledgeId(),
                    ignored -> toReferenceItem(knowledge, customerNameMap.get(knowledge.getCustomerId()))
                );
                item.setMatchPercent(Math.max(item.getMatchPercent(), toMatchPercent(chunk.getScore())));
                if (StrUtil.isBlank(item.getExcerpt())) {
                    item.setExcerpt(extractSnippet(chunk.getContent(), keyword));
                }
            }

            return deduped.values().stream()
                .sorted(Comparator.comparingInt(KnowledgeAiSearchVO.ReferenceItem::getMatchPercent).reversed()
                    .thenComparing(KnowledgeAiSearchVO.ReferenceItem::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
        } catch (Exception e) {
            log.warn("知识库语义检索失败，回退到本地内容搜索: {}", e.getMessage());
            return List.of();
        }
    }

    private List<KnowledgeAiSearchVO.ReferenceItem> buildLocalReferences(String keyword, String type, int limit) {
        KnowledgeQueryBO queryBO = new KnowledgeQueryBO();
        queryBO.setKeyword(keyword);
        queryBO.setType(type);
        queryBO.setPage(1);
        queryBO.setLimit(limit);

        BasePage<KnowledgeVO> page = queryPageList(queryBO);
        List<KnowledgeAiSearchVO.ReferenceItem> references = new ArrayList<>();
        for (KnowledgeVO knowledge : page.getList()) {
            KnowledgeAiSearchVO.ReferenceItem item = new KnowledgeAiSearchVO.ReferenceItem();
            item.setKnowledgeId(knowledge.getKnowledgeId());
            item.setName(knowledge.getName());
            item.setType(knowledge.getType());
            item.setCustomerName(knowledge.getCustomerName());
            item.setSummary(knowledge.getSummary());
            item.setExcerpt(extractSnippet(knowledge.getContentText(), keyword));
            item.setMatchPercent(calculateLocalMatchPercent(keyword, knowledge));
            item.setFileSize(knowledge.getFileSize());
            item.setCreateTime(knowledge.getCreateTime());
            references.add(item);
        }
        return references;
    }

    private String buildAiSearchAnswer(String keyword, List<KnowledgeAiSearchVO.ReferenceItem> references) {
        if (references.isEmpty()) {
            return "### 未找到相关内容\n\n当前知识库里还没有与“" + keyword + "”直接相关的资料，可以换个关键词再试试。";
        }

        String fallback = buildFallbackAiAnswer(keyword, references);
        if (!chatClientProvider.isApiKeyConfigured()) {
            return fallback;
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < references.size(); i++) {
            KnowledgeAiSearchVO.ReferenceItem item = references.get(i);
            contextBuilder.append("资料").append(i + 1).append("：").append(item.getName()).append("\n");
            if (StrUtil.isNotBlank(item.getSummary())) {
                contextBuilder.append("摘要：").append(item.getSummary()).append("\n");
            }
            if (StrUtil.isNotBlank(item.getExcerpt())) {
                contextBuilder.append("命中片段：").append(item.getExcerpt()).append("\n");
            }
            contextBuilder.append("\n");
        }

        String prompt = """
            你是企业知识库搜索助手。请基于以下检索到的资料，用中文输出一段简洁、专业、可直接展示给业务用户的答案。

            要求：
            1. 只基于提供的资料回答，不要编造。
            2. 输出使用 Markdown。
            3. 先给结论，再分点列出关键依据。
            4. 如果资料不足以得出确定结论，要明确说明信息不足。
            5. 不要输出“根据资料1/资料2”这类生硬措辞，改成自然表达。

            用户问题：
            %s

            检索资料：
            %s
            """.formatted(keyword, contextBuilder);

        try {
            String answer = chatClientProvider.getChatClient()
                .prompt()
                .user(prompt)
                .call()
                .content();
            return StrUtil.isNotBlank(answer) ? answer.trim() : fallback;
        } catch (Exception e) {
            log.warn("生成知识库 AI 检索答案失败，使用兜底摘要: {}", e.getMessage());
            return fallback;
        }
    }

    private String buildFallbackAiAnswer(String keyword, List<KnowledgeAiSearchVO.ReferenceItem> references) {
        StringBuilder answer = new StringBuilder();
        answer.append("### “").append(keyword).append("”相关知识解答\n\n");
        KnowledgeAiSearchVO.ReferenceItem top = references.getFirst();
        if (StrUtil.isNotBlank(top.getSummary())) {
            answer.append(top.getSummary()).append("\n\n");
        } else if (StrUtil.isNotBlank(top.getExcerpt())) {
            answer.append(top.getExcerpt()).append("\n\n");
        } else {
            answer.append("已找到与该主题相关的知识资料，建议优先查看下方参考文档。\n\n");
        }

        answer.append("#### 关键信息\n");
        for (KnowledgeAiSearchVO.ReferenceItem item : references.stream().limit(3).toList()) {
            answer.append("- **").append(item.getName()).append("**");
            if (StrUtil.isNotBlank(item.getExcerpt())) {
                answer.append("：").append(item.getExcerpt());
            } else if (StrUtil.isNotBlank(item.getSummary())) {
                answer.append("：").append(item.getSummary());
            }
            answer.append("\n");
        }
        return answer.toString().trim();
    }

    private Map<Long, String> loadCustomerNameMap(List<Knowledge> knowledges) {
        Set<Long> customerIds = new LinkedHashSet<>();
        for (Knowledge knowledge : knowledges) {
            if (knowledge.getCustomerId() != null) {
                customerIds.add(knowledge.getCustomerId());
            }
        }
        if (customerIds.isEmpty()) {
            return Map.of();
        }

        List<Customer> customers = customerMapper.selectBatchIds(customerIds);
        Map<Long, String> customerNameMap = new LinkedHashMap<>();
        for (Customer customer : customers) {
            customerNameMap.put(customer.getCustomerId(), customer.getCompanyName());
        }
        return customerNameMap;
    }

    private KnowledgeAiSearchVO.ReferenceItem toReferenceItem(Knowledge knowledge, String customerName) {
        KnowledgeAiSearchVO.ReferenceItem item = new KnowledgeAiSearchVO.ReferenceItem();
        item.setKnowledgeId(knowledge.getKnowledgeId());
        item.setName(knowledge.getName());
        item.setType(knowledge.getType());
        item.setCustomerName(customerName);
        item.setSummary(knowledge.getSummary());
        item.setExcerpt(extractSnippet(knowledge.getContentText(), null));
        item.setMatchPercent(0);
        item.setFileSize(knowledge.getFileSize());
        item.setCreateTime(knowledge.getCreateTime());
        return item;
    }

    private int calculateOverallMatchPercent(List<KnowledgeAiSearchVO.ReferenceItem> references) {
        if (references.isEmpty()) {
            return 0;
        }
        return references.stream()
            .map(KnowledgeAiSearchVO.ReferenceItem::getMatchPercent)
            .max(Integer::compareTo)
            .orElse(0);
    }

    private int calculateLocalMatchPercent(String keyword, KnowledgeVO knowledge) {
        int score = 55;
        if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(knowledge.getName(), ""), keyword)) {
            score += 20;
        }
        if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(knowledge.getSummary(), ""), keyword)) {
            score += 12;
        }
        if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(knowledge.getContentText(), ""), keyword)) {
            score += 18;
        }
        return Math.min(score, 98);
    }

    private int toMatchPercent(Double score) {
        if (score == null) {
            return 86;
        }
        if (score <= 1) {
            return Math.max(1, Math.min(99, (int) Math.round(score * 100)));
        }
        return Math.max(1, Math.min(99, (int) Math.round(score)));
    }

    private String extractSnippet(String content, String keyword) {
        String normalized = normalizeSearchableContent(content);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        if (keyword != null) {
            int index = normalized.toLowerCase().indexOf(keyword.toLowerCase());
            if (index >= 0) {
                int start = Math.max(0, index - 60);
                int end = Math.min(normalized.length(), index + keyword.length() + 120);
                String snippet = normalized.substring(start, end).trim();
                if (start > 0) {
                    snippet = "..." + snippet;
                }
                if (end < normalized.length()) {
                    snippet = snippet + "...";
                }
                return snippet;
            }
        }
        return normalized.length() > MAX_AI_SEARCH_CONTEXT_LENGTH
            ? normalized.substring(0, MAX_AI_SEARCH_CONTEXT_LENGTH) + "..."
            : normalized;
    }

    private String extractSearchableContent(MultipartFile file) {
        if (file == null) {
            return null;
        }
        try (InputStream inputStream = file.getInputStream()) {
            return extractSearchableContent(inputStream, file.getContentType(), file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("提取知识库文件正文失败: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    private String extractSearchableContent(String filePath, String mimeType, String fileName) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
            return extractSearchableContent(inputStream, mimeType, fileName);
        } catch (Exception e) {
            log.warn("提取知识库文件正文失败: {}", fileName, e);
            return null;
        }
    }

    private String extractSearchableContent(InputStream inputStream, String mimeType, String fileName) throws Exception {
        if (inputStream == null) {
            return null;
        }

        String text;
        if (isPlainTextFile(mimeType, fileName)) {
            byte[] bytes = inputStream.readNBytes(MAX_SEARCHABLE_CONTENT_LENGTH * 3);
            text = new String(bytes, StandardCharsets.UTF_8);
        } else if (isDocumentFile(mimeType, fileName)) {
            text = tika.parseToString(inputStream);
        } else {
            return null;
        }

        return normalizeSearchableContent(text);
    }

    private String normalizeSearchableContent(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() > MAX_SEARCHABLE_CONTENT_LENGTH) {
            normalized = normalized.substring(0, MAX_SEARCHABLE_CONTENT_LENGTH);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private boolean isPlainTextFile(String mimeType, String fileName) {
        if (mimeType != null && (mimeType.startsWith("text/") || "application/json".equals(mimeType))) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt")
            || lower.endsWith(".md")
            || lower.endsWith(".markdown")
            || lower.endsWith(".csv")
            || lower.endsWith(".json")
            || lower.endsWith(".xml")
            || lower.endsWith(".yaml")
            || lower.endsWith(".yml")
            || lower.endsWith(".log");
    }

    private boolean isDocumentFile(String mimeType, String fileName) {
        if (mimeType != null) {
            if ("application/pdf".equals(mimeType)
                || "application/msword".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)
                || "application/vnd.ms-excel".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType)
                || "application/vnd.ms-powerpoint".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.presentationml.presentation".equals(mimeType)) {
                return true;
            }
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf")
            || lower.endsWith(".doc")
            || lower.endsWith(".docx")
            || lower.endsWith(".xls")
            || lower.endsWith(".xlsx")
            || lower.endsWith(".ppt")
            || lower.endsWith(".pptx");
    }
}
