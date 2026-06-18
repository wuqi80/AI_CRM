package com.kakarote.ai_crm.config;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

/**
 * 异步配置类
 * 启用 @Async 注解支持，用于异步上传文件到 WeKnora
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 处理customerAiAnalysisExecutor方法逻辑。
     */
    @Bean(name = "customerAiAnalysisExecutor")
    public Executor customerAiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("customer-ai-analysis-");
        executor.setTaskDecorator(contextPropagatingTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    @Bean(name = "accessLogExecutor")
    public Executor accessLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("access-log-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    private TaskDecorator contextPropagatingTaskDecorator() {
        return task -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long aiSessionId = AiContextHolder.getCurrentSessionId();
            return () -> {
                try {
                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                    securityContext.setAuthentication(authentication);
                    SecurityContextHolder.setContext(securityContext);
                    if (aiSessionId != null) {
                        AiContextHolder.setSessionId(aiSessionId);
                    }
                    task.run();
                } finally {
                    SecurityContextHolder.clearContext();
                    DataPermissionHolder.clear();
                    AiContextHolder.clearThreadContext();
                }
            };
        };
    }

}
