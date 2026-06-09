package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.service.impl.WecomArchiveEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业微信「会话内容存档 - 接收事件服务器」回调端点（{@code /wecom/archive/callback}）。
 *
 * <p>近实时自动同步入口：企微在有新存档消息时推送事件到此端点，触发对应企业的增量拉取
 * （拉取仍走 WeWorkFinanceSDK getchatdata）。GET 做 URL 有效性验证，POST 解析事件后异步拉取并回 success。
 * 与 {@code /wecom/open/callback}（第三方）、{@code /wecom/dev/callback}（代开发）相互独立，
 * 使用会话存档自己的一套 Token/EncodingAESKey。需在 SecurityConfig 放行本路径。</p>
 */
@RestController
@RequestMapping("/wecom/archive")
@Tag(name = "WeCom Archive Event Callback")
public class WecomArchiveCallbackController {

    @Autowired
    private WecomArchiveEventService eventService;

    @GetMapping("/callback")
    @Operation(summary = "Verify WeCom archive event callback URL")
    public ResponseEntity<String> verify(@RequestParam(value = "msg_signature", required = false) String msgSignature,
                                         @RequestParam(required = false) String timestamp,
                                         @RequestParam(required = false) String nonce,
                                         @RequestParam(value = "echostr", required = false) String echoStr) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .body(eventService.verifyUrl(msgSignature, timestamp, nonce, echoStr));
    }

    @PostMapping("/callback")
    @Operation(summary = "Receive WeCom archive new-message event")
    public ResponseEntity<String> callback(@RequestBody(required = false) String body,
                                           @RequestParam(value = "msg_signature", required = false) String msgSignature,
                                           @RequestParam(required = false) String timestamp,
                                           @RequestParam(required = false) String nonce) {
        eventService.onEvent(body, msgSignature, timestamp, nonce);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
    }
}
