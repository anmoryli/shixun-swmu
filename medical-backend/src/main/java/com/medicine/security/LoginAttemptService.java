/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败计数与账号锁定。
 * 基于 Redis 按用户名维度累计失败次数，超过阈值后锁定一段时间，防止暴力破解。
 */
@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "medicine:auth:loginfail:";

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(StringRedisTemplate redisTemplate,
                               @Value("${app.auth.login-max-attempts:5}") int maxAttempts,
                               @Value("${app.auth.login-lock-duration:15m}") Duration lockDuration) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + username.trim());
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= maxAttempts;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public void recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String key = KEY_PREFIX + username.trim();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, lockDuration);
        }
    }

    public void clear(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        redisTemplate.delete(KEY_PREFIX + username.trim());
    }
}
