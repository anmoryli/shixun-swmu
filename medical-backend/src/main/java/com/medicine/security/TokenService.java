/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String tokenPrefix;
    private final Duration tokenTtl;

    public TokenService(StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper,
                        @Value("${app.auth.token-prefix}") String tokenPrefix,
                        @Value("${app.auth.token-ttl}") Duration tokenTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tokenPrefix = tokenPrefix;
        this.tokenTtl = tokenTtl;
    }

    /**
     * 计算 token 的 SHA-256 摘要，作为 Redis 存储 key 的后缀。
     * 原始 token 仅下发给客户端 cookie，Redis 侧不可还原，降低会话泄露风险。
     */
    private String digest(String token) {
        try {
            MessageDigest md = messageDigest();
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "登录会话初始化失败");
        }
    }

    MessageDigest messageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    public String create(AuthSession session) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(tokenPrefix + digest(token), objectMapper.writeValueAsString(session), tokenTtl);
            return token;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建登录会话失败");
        }
    }

    public Optional<AuthSession> find(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        String key = tokenPrefix + digest(token);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            AuthSession session = objectMapper.readValue(value, AuthSession.class);
            // 滑动续期：每次命中刷新 TTL，活跃用户不掉线，7 天不活动才过期。
            redisTemplate.expire(key, tokenTtl);
            return Optional.of(session);
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public void delete(String token) {
        if (token != null && !token.trim().isEmpty()) {
            redisTemplate.delete(tokenPrefix + digest(token));
        }
    }

    public static Optional<String> normalizeAuthorization(String authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        String token = authorization.trim();
        if (token.equalsIgnoreCase("Bearer")) {
            return Optional.empty();
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
