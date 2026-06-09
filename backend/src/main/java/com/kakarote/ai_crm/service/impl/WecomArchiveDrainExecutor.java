package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.mapper.WecomCorpConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会话存档增量拉取的异步执行器。
 *
 * <p>独立成 Bean 是为了让 {@code @Async} 跨 Bean 调用真正生效（避免自调用绕过代理），
 * 并集中处理「无请求上下文的异步线程」的租户上下文设置/清理（见 CLAUDE.md：异步任务必须
 * 手动设置 TenantContextHolder 并在 finally 清理）。</p>
 */
@Component
public class WecomArchiveDrainExecutor {

    private static final Logger log = LoggerFactory.getLogger(WecomArchiveDrainExecutor.class);

    @Autowired
    private WecomCorpConfigMapper configMapper;

    @Autowired
    private WecomSyncServiceImpl syncService;

    /**
     * 按 corpId 异步增量拉取会话存档。corpId 可能是真实 corpid 或第三方回写的 corp_id，
     * 故按 (corp_id OR archive_corp_id) 匹配所有已启用存档的授权企业（跨租户）。
     */
    @Async("wecomArchiveExecutor")
    public void drainByCorpId(String corpId, int maxPages) {
        List<WecomCorpConfig> targets = configMapper.selectArchiveTargetsByCorpIdIgnoreTenant(corpId);
        if (targets == null || targets.isEmpty()) {
            log.debug("WeCom archive event: no archive-enabled corp matched corpId={}", corpId);
            return;
        }
        for (WecomCorpConfig config : targets) {
            if (config.getTenantId() == null) {
                log.warn("WeCom archive target has null tenantId, skipped: corpId={}", corpId);
                continue;
            }
            try {
                TenantContextHolder.setTenantId(config.getTenantId());
                int saved = syncService.drainArchive(config, maxPages);
                log.info("WeCom archive drained by event: corpId={}, tenantId={}, saved={}",
                        corpId, config.getTenantId(), saved);
            } catch (Exception e) {
                log.warn("WeCom archive drain failed: corpId={}, tenantId={}, err={}",
                        corpId, config.getTenantId(), e.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        }
    }
}
