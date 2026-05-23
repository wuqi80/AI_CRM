package com.kakarote.ai_crm.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicChatClientProviderTest {

    private final DynamicChatClientProvider provider = new DynamicChatClientProvider();

    @Test
    void buildChatOptionsDisablesThinkingForDashscope() {
        OpenAiChatOptions options = provider.buildChatOptions("dashscope", "qwen3.6-plus", 0.7, 2048);

        assertThat(options.getStreamUsage()).isTrue();
        assertThat(options.getExtraBody()).containsEntry("enable_thinking", Boolean.FALSE);
    }

    @Test
    void buildChatOptionsDoesNotAddThinkingFlagForOtherProviders() {
        OpenAiChatOptions options = provider.buildChatOptions("openai", "gpt-5.4", 0.7, 2048);

        assertThat(options.getExtraBody()).isNullOrEmpty();
    }

    @Test
    void resolveActualRequestBaseUrlUsesConfiguredOpenAiProxy() {
        String actualBaseUrl = DynamicChatClientProvider.resolveActualRequestBaseUrl(
            "openai", "https://api.openai.com", "http://127.0.0.1:9000");

        assertThat(actualBaseUrl).isEqualTo("http://127.0.0.1:9000");
    }
}
