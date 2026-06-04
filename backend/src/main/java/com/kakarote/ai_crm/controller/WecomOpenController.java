package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.VO.WecomOpenAuthorizeVO;
import com.kakarote.ai_crm.service.impl.WecomOpenPlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/wecom/open")
@Tag(name = "WeCom Open Platform")
public class WecomOpenController {

    @Autowired
    private WecomOpenPlatformService openPlatformService;

    @GetMapping("/authorize")
    @Operation(summary = "Create WeCom third-party authorize URL")
    @RequirePermission("config:ai")
    public Result<WecomOpenAuthorizeVO> authorize(@RequestParam(required = false) String redirect,
                                                  HttpServletRequest request) {
        return Result.ok(openPlatformService.createAuthorizeUrl(redirect, request));
    }

    @GetMapping("/auth/callback")
    @Operation(summary = "WeCom third-party auth callback")
    public void authCallback(@RequestParam(value = "auth_code", required = false) String authCode,
                             @RequestParam(value = "code", required = false) String code,
                             @RequestParam(required = false) String state,
                             @RequestParam(required = false) String error,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        response.sendRedirect(openPlatformService.handleAuthCallback(authCode == null ? code : authCode, state, error, request));
    }

    @GetMapping("/callback")
    @Operation(summary = "Verify WeCom third-party callback URL")
    public ResponseEntity<String> verifyCallback(@RequestParam(value = "msg_signature", required = false) String msgSignature,
                                                 @RequestParam(required = false) String timestamp,
                                                 @RequestParam(required = false) String nonce,
                                                 @RequestParam(value = "echostr", required = false) String echoStr) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(openPlatformService.verifyCallbackUrl(msgSignature, timestamp, nonce, echoStr));
    }

    @PostMapping("/callback")
    @Operation(summary = "Receive WeCom third-party callback event")
    public ResponseEntity<String> callback(@RequestBody String body,
                                           @RequestParam(value = "msg_signature", required = false) String msgSignature,
                                           @RequestParam(required = false) String timestamp,
                                           @RequestParam(required = false) String nonce) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(openPlatformService.handleCallback(body, msgSignature, timestamp, nonce));
    }
}
