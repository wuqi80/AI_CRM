package com.kakarote.ai_crm.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/legal-document")
public class LegalDocumentController {

    private static final String AGREEMENT_URL = "https://file.72crm.com/static/law/72crm_ai_service.txt";
    private static final String PRIVACY_URL = "https://file.72crm.com/static/law/72crm_ai_privacy.txt";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final MediaType UTF8_TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @GetMapping(value = "/{type}", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> getLegalDocument(@PathVariable String type) {
        URI documentUri = resolveDocumentUri(type);
        HttpRequest request = HttpRequest.newBuilder(documentUri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "法律文档加载失败");
            }

            return ResponseEntity.ok()
                    .contentType(UTF8_TEXT)
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES).cachePublic())
                    .body(new String(response.body(), StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "法律文档加载被中断", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "法律文档加载失败", e);
        }
    }

    private URI resolveDocumentUri(String type) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "agreement" -> URI.create(AGREEMENT_URL);
            case "privacy" -> URI.create(PRIVACY_URL);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "法律文档不存在");
        };
    }
}
