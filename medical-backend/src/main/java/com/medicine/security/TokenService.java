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

import java.time.Duration;
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

    public String create(AuthSession session) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(tokenPrefix + token, objectMapper.writeValueAsString(session), tokenTtl);
            return token;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建登录会话失败");
        }
    }

    public Optional<AuthSession> find(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        String value = redisTemplate.opsForValue().get(tokenPrefix + token);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, AuthSession.class));
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(tokenPrefix + token);
            return Optional.empty();
        }
    }

    public void delete(String token) {
        if (token != null && !token.trim().isEmpty()) {
            redisTemplate.delete(tokenPrefix + token);
        }
    }

    public static Optional<String> normalizeAuthorization(String authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        String token = authorization.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
