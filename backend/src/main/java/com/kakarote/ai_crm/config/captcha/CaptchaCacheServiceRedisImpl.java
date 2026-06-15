package com.kakarote.ai_crm.config.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import com.kakarote.ai_crm.common.redis.Redis;

public class CaptchaCacheServiceRedisImpl implements CaptchaCacheService {

    private Redis redis;

    @Override
    public String type() {
        return "redis";
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        redis.setex(key, Math.toIntExact(expiresInSeconds), value);
    }

    @Override
    public boolean exists(String key) {
        return redis.exists(key);
    }

    @Override
    public void delete(String key) {
        redis.del(key);
    }

    @Override
    public String get(String key) {
        return redis.get(key);
    }

    @Override
    public Long increment(String key, long val) {
        return redis.incr(key, val);
    }
}
