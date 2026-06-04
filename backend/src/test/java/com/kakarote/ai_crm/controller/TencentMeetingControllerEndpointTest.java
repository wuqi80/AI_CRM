package com.kakarote.ai_crm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TencentMeetingControllerEndpointTest {

    @Test
    void configEndpointShouldNotBeExposed() {
        var exposedConfigMethods = Arrays.stream(TencentMeetingController.class.getDeclaredMethods())
                .filter(method -> mappingPaths(method).anyMatch("/config"::equals))
                .map(Method::getName)
                .toList();

        assertThat(exposedConfigMethods).isEmpty();
    }

    @Test
    void oauthAccountsEndpointsShouldNotBeExposed() {
        var exposedAccountMethods = Arrays.stream(TencentMeetingController.class.getDeclaredMethods())
                .filter(method -> mappingPaths(method).anyMatch(path -> path.startsWith("/oauth/accounts")))
                .map(Method::getName)
                .toList();

        assertThat(exposedAccountMethods).isEmpty();
    }

    @Test
    void webhookEndpointShouldBeAllowedWithoutLogin() throws Exception {
        String securityConfig = Files.readString(Path.of(
                "src/main/java/com/kakarote/ai_crm/config/SecurityConfig.java"));

        assertThat(securityConfig).contains("\"/tencent-meeting/webhook\"");
    }

    private Stream<String> mappingPaths(Method method) {
        Stream<String> getPaths = method.isAnnotationPresent(GetMapping.class)
                ? Stream.concat(Arrays.stream(method.getAnnotation(GetMapping.class).value()),
                Arrays.stream(method.getAnnotation(GetMapping.class).path()))
                : Stream.empty();
        Stream<String> postPaths = method.isAnnotationPresent(PostMapping.class)
                ? Stream.concat(Arrays.stream(method.getAnnotation(PostMapping.class).value()),
                Arrays.stream(method.getAnnotation(PostMapping.class).path()))
                : Stream.empty();
        return Stream.concat(getPaths, postPaths);
    }
}
