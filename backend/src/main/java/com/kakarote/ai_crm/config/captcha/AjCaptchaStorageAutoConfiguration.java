package com.kakarote.ai_crm.config.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.kakarote.ai_crm.common.redis.Redis;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AjCaptchaStorageAutoConfiguration {

    @Bean(name = "AjCaptchaCacheService")
    public CaptchaCacheService captchaCacheService(AjCaptchaProperties ajCaptchaProperties, Redis redis) {
        CaptchaCacheService cacheService = CaptchaServiceFactory.getCache(ajCaptchaProperties.getCacheType().name());
        if (cacheService instanceof CaptchaCacheServiceRedisImpl redisCacheService) {
            redisCacheService.setRedis(redis);
        }
        return cacheService;
    }
}
