package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.service.AiAudioTranscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiAudioTranscriptionServiceImpl implements AiAudioTranscriptionService {

    private static final String OPENAI_TRANSCRIPTION_MODEL = "gpt-4o-mini-transcribe";
    private static final String DASHSCOPE_TRANSCRIPTION_MODEL = "qwen3-asr-flash";
    private static final String OPENAI_TRANSCRIPTION_PROMPT =
        "Please transcribe the audio accurately as Simplified Chinese text. Return only the transcript.";
    private static final String UNSUPPORTED_PROVIDER_MESSAGE =
        "The current model does not support audio transcription. Please configure OpenAI or DashScope.";

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Please upload an audio file first");
        }
        try {
            return transcribe(
                audioFile.getBytes(),
                StrUtil.blankToDefault(audioFile.getOriginalFilename(), "followup-audio.webm"),
                audioFile.getContentType()
            );
        } catch (IOException ex) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Audio transcription failed");
        }
    }

    @Override
    public String transcribe(byte[] audioBytes, String filename, String contentType) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Please upload an audio file first");
        }

        DynamicChatClientProvider.AiRuntimeConfigSnapshot runtimeConfig =
            chatClientProvider.getCurrentRuntimeConfigSnapshot();
        if (runtimeConfig == null || StrUtil.isBlank(runtimeConfig.apiKey())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Please configure an AI API key first");
        }

        AiModelCapabilities capabilities = runtimeConfig.capabilities();
        if (capabilities == null || !capabilities.isSupportsAudioTranscription()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, UNSUPPORTED_PROVIDER_MESSAGE);
        }

        String providerCode = StrUtil.blankToDefault(runtimeConfig.providerCode(), "").trim().toLowerCase();
        try {
            String transcript = switch (providerCode) {
                case "openai" -> transcribeWithOpenAi(runtimeConfig, audioBytes, filename, contentType);
                case "dashscope" -> transcribeWithDashscope(runtimeConfig, audioBytes, contentType);
                default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, UNSUPPORTED_PROVIDER_MESSAGE);
            };
            if (StrUtil.isBlank(transcript)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Audio transcription result is empty");
            }
            return transcript.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI audio transcription failed: provider={}", providerCode, ex);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Audio transcription failed");
        }
    }

    private String transcribeWithOpenAi(DynamicChatClientProvider.AiRuntimeConfigSnapshot runtimeConfig,
                                        byte[] audioBytes,
                                        String filename,
                                        String contentType) {
        String endpoint = DynamicChatClientProvider.resolveActualRequestBaseUrl(
            runtimeConfig.providerCode(),
            runtimeConfig.apiUrl()
        ) + "/v1/audio/transcriptions";

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("model", OPENAI_TRANSCRIPTION_MODEL);
        bodyBuilder.part("response_format", "text");
        bodyBuilder.part("prompt", OPENAI_TRANSCRIPTION_PROMPT);
        bodyBuilder.part("file", asResource(audioBytes, filename)).contentType(resolveMediaType(contentType));

        return webClient.post()
            .uri(endpoint)
            .headers(headers -> applyRequestHeaders(headers, runtimeConfig))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse ->
                clientResponse.bodyToMono(String.class).map(this::toBusinessException))
            .bodyToMono(String.class)
            .block();
    }

    private String transcribeWithDashscope(DynamicChatClientProvider.AiRuntimeConfigSnapshot runtimeConfig,
                                           byte[] audioBytes,
                                           String contentType) {
        String endpoint = DynamicChatClientProvider.resolveActualRequestBaseUrl(
            runtimeConfig.providerCode(),
            runtimeConfig.apiUrl()
        ) + "/v1/chat/completions";

        String mimeType = resolveMediaType(contentType).toString();
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(audioBytes);
        Map<String, Object> body = Map.of(
            "model", DASHSCOPE_TRANSCRIPTION_MODEL,
            "messages", List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of(
                    "type", "input_audio",
                    "input_audio", Map.of("data", dataUri)
                ))
            )),
            "stream", false,
            "asr_options", Map.of("enable_itn", false)
        );

        String response = webClient.post()
            .uri(endpoint)
            .headers(headers -> applyRequestHeaders(headers, runtimeConfig))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse ->
                clientResponse.bodyToMono(String.class).map(this::toBusinessException))
            .bodyToMono(String.class)
            .block();

        return extractDashscopeTranscript(response);
    }

    private ByteArrayResource asResource(byte[] data, String filename) {
        String actualFilename = StrUtil.blankToDefault(filename, "followup-audio.webm");
        return new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return actualFilename;
            }
        };
    }

    private MediaType resolveMediaType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private void applyRequestHeaders(HttpHeaders headers,
                                     DynamicChatClientProvider.AiRuntimeConfigSnapshot runtimeConfig) {
        headers.setBearerAuth(runtimeConfig.apiKey());
        parseExtraHeaders(runtimeConfig.extraHeadersJson()).forEach(headers::set);
    }

    private Map<String, String> parseExtraHeaders(String extraHeadersJson) {
        if (StrUtil.isBlank(extraHeadersJson)) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(
                extraHeadersJson,
                new TypeReference<Map<String, Object>>() {
                }
            );
            return values.entrySet().stream()
                .filter(entry -> StrUtil.isNotBlank(entry.getKey()) && entry.getValue() != null)
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().trim(),
                    entry -> String.valueOf(entry.getValue())
                ));
        } catch (Exception ex) {
            log.warn("Failed to parse extra headers for audio transcription: {}", ex.getMessage());
            return Map.of();
        }
    }

    private String extractDashscopeTranscript(String responseBody) {
        try {
            JsonNode contentNode = objectMapper.readTree(responseBody)
                .path("choices")
                .path(0)
                .path("message")
                .path("content");
            if (contentNode.isTextual()) {
                return contentNode.asText("").trim();
            }
            if (contentNode.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode item : contentNode) {
                    if (item.isTextual()) {
                        builder.append(item.asText());
                    } else if (item.hasNonNull("text")) {
                        builder.append(item.get("text").asText());
                    }
                }
                return builder.toString().trim();
            }
        } catch (Exception ex) {
            log.warn("Failed to parse DashScope transcription response: {}", ex.getMessage());
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Audio transcription result is empty");
    }

    private BusinessException toBusinessException(String responseBody) {
        return new BusinessException(SystemCodeEnum.SYSTEM_ERROR, extractErrorMessage(responseBody));
    }

    private String extractErrorMessage(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "Audio transcription failed";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.hasNonNull("error")) {
                JsonNode errorNode = root.get("error");
                if (errorNode.isTextual()) {
                    return errorNode.asText();
                }
                if (errorNode.hasNonNull("message")) {
                    return errorNode.get("message").asText();
                }
                if (errorNode.hasNonNull("msg")) {
                    return errorNode.get("msg").asText();
                }
            }
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
            if (root.hasNonNull("msg")) {
                return root.get("msg").asText();
            }
        } catch (Exception ex) {
            log.warn("Failed to parse provider error response: {}", ex.getMessage());
        }
        return "Audio transcription failed";
    }
}
