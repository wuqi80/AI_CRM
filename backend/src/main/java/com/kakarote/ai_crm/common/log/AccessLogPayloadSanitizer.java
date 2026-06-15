package com.kakarote.ai_crm.common.log;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class AccessLogPayloadSanitizer {

    public static final int REQUEST_BODY_LIMIT_BYTES = 64 * 1024;
    private static final int TEXT_LIMIT_CHARS = 16 * 1024;
    private static final String REDACTED = "***";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "manager-token",
            "token",
            "password",
            "oldpassword",
            "newpassword",
            "apikey",
            "api_key",
            "api-key",
            "secret",
            "client_secret",
            "captcha",
            "captchaverification",
            "verificationcode"
    );
    private static final Pattern JSON_SECRET_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|oldPassword|newPassword|token|apiKey|api_key|secret|clientSecret|client_secret|captcha|captchaVerification|verificationCode)\"\\s*:\\s*\")([^\"]*)(\")"
    );
    private static final Pattern FORM_SECRET_PATTERN = Pattern.compile(
            "(?i)(^|[&?])((?:password|oldPassword|newPassword|token|apiKey|api_key|secret|clientSecret|client_secret|captcha|captchaVerification|verificationCode)=)([^&]*)"
    );

    private AccessLogPayloadSanitizer() {
    }

    public static String sanitizeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            String value = isSensitiveKey(name)
                    ? REDACTED
                    : StrUtil.maxLength(request.getHeader(name), 500);
            headers.put(name, value);
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(headers);
        } catch (Exception ignored) {
            return headers.toString();
        }
    }

    public static SanitizedText sanitizeQueryString(String queryString) {
        if (StrUtil.isBlank(queryString)) {
            return new SanitizedText(null, false);
        }
        String decoded = decodeUrl(queryString);
        return truncateToChars(redactFormLikeText(decoded), TEXT_LIMIT_CHARS);
    }

    public static SanitizedText sanitizeRequestBody(String body, String contentType) {
        if (StrUtil.isBlank(body)) {
            return new SanitizedText(null, false);
        }

        String normalizedContentType = StrUtil.nullToEmpty(contentType).toLowerCase(Locale.ROOT);
        String sanitized = body;
        if (normalizedContentType.contains("json")) {
            sanitized = JSON_SECRET_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED + "$3");
        } else if (normalizedContentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            sanitized = redactFormLikeText(sanitized);
        }
        return truncateToChars(sanitized, TEXT_LIMIT_CHARS);
    }

    public static ResultResponseSummary summarizeResultResponse(String body) {
        if (StrUtil.isBlank(body)) {
            return new ResultResponseSummary(null, null, false, false);
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode codeNode = root.get("code");
            JsonNode msgNode = root.get("msg");
            if (codeNode == null || !codeNode.canConvertToInt()) {
                return new ResultResponseSummary(null, null, false, false);
            }

            Integer code = codeNode.asInt();
            String msg = msgNode != null && !msgNode.isNull() ? msgNode.asText() : null;
            String summary = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "code", code,
                    "msg", StrUtil.blankToDefault(msg, "")
            ));
            SanitizedText truncated = truncateToChars(summary, TEXT_LIMIT_CHARS);
            return new ResultResponseSummary(truncated.value(), code, truncated.truncated(), true);
        } catch (Exception ignored) {
            SanitizedText truncated = truncateToChars(body, TEXT_LIMIT_CHARS);
            return new ResultResponseSummary(truncated.value(), null, truncated.truncated(), false);
        }
    }

    public static String decodeBody(byte[] bytes, String characterEncoding) {
        Charset charset = StandardCharsets.UTF_8;
        if (StrUtil.isNotBlank(characterEncoding)) {
            try {
                charset = Charset.forName(characterEncoding);
            } catch (Exception ignored) {
                charset = StandardCharsets.UTF_8;
            }
        }
        return new String(bytes, charset);
    }

    public static SanitizedText truncateToChars(String text, int limit) {
        if (text == null || limit <= 0 || text.length() <= limit) {
            return new SanitizedText(text, false);
        }
        return new SanitizedText(text.substring(0, limit), true);
    }

    public static String buildOmittedBodyMessage(String contentType, long contentLength) {
        if (contentLength <= 0) {
            return null;
        }
        return "[request body omitted: contentType=" + StrUtil.blankToDefault(contentType, "unknown")
                + ", contentLength=" + contentLength + "]";
    }

    private static String redactFormLikeText(String text) {
        return FORM_SECRET_PATTERN.matcher(text).replaceAll("$1$2" + REDACTED);
    }

    private static String decodeUrl(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }

    private static boolean isSensitiveKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        String normalized = key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT))
                || SENSITIVE_KEYS.contains(normalized)
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("password");
    }

    public record SanitizedText(String value, boolean truncated) {
    }

    public record ResultResponseSummary(
            String responseBody,
            Integer businessCode,
            boolean truncated,
            boolean resultResponse
    ) {
    }
}
