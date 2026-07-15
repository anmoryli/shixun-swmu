/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class TokenServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private TokenService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new TokenService(redisTemplate, objectMapper, "session:", Duration.ofHours(8));
    }

    @Test
    void acceptsLegacyRawAuthorizationToken() {
        assertThat(TokenService.normalizeAuthorization("  abc123  ")).hasValue("abc123");
    }

    @Test
    void alsoAcceptsBearerToken() {
        assertThat(TokenService.normalizeAuthorization("Bearer abc123")).hasValue("abc123");
    }

    @Test
    void rejectsMissingAndEmptyAuthorization() {
        assertThat(TokenService.normalizeAuthorization(null)).isEmpty();
        assertThat(TokenService.normalizeAuthorization("  ")).isEmpty();
        assertThat(TokenService.normalizeAuthorization("Bearer  ")).isEmpty();
    }

    @Test
    void storesOnlyTokenDigestWithSerializedSessionAndTtl() throws Exception {
        AuthSession session = session();

        String token = service.create(session);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(key.capture(), json.capture(), ttl.capture());
        assertThat(token).hasSize(32).doesNotContain("-");
        assertThat(key.getValue()).startsWith("session:").hasSize("session:".length() + 64);
        assertThat(key.getValue()).doesNotContain(token);
        assertThat(objectMapper.readValue(json.getValue(), AuthSession.class).getUsername()).isEqualTo("admin_1");
        assertThat(ttl.getValue()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void returnsSessionAndRefreshesTtlForAValidToken() throws Exception {
        when(valueOperations.get(anyString())).thenReturn(objectMapper.writeValueAsString(session()));

        assertThat(service.find("raw-token")).get().extracting(AuthSession::getRoleName).isEqualTo("ROLE_1");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).get(key.capture());
        verify(redisTemplate).expire(key.getValue(), Duration.ofHours(8));
        assertThat(key.getValue()).doesNotContain("raw-token");
    }

    @Test
    void returnsEmptyForMissingAndBlankTokensWithoutRedisLookup() {
        assertThat(service.find(null)).isEmpty();
        assertThat(service.find(" ")).isEmpty();

        verifyNoInteractions(valueOperations);
    }

    @Test
    void removesCorruptSessionData() {
        when(valueOperations.get(anyString())).thenReturn("not-json");

        assertThat(service.find("raw-token")).isEmpty();

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).get(key.capture());
        verify(redisTemplate).delete(key.getValue());
        verify(redisTemplate, never()).expire(anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void deletesOnlyNonBlankTokens() {
        service.delete(null);
        service.delete(" ");
        verify(redisTemplate, never()).delete(anyString());

        service.delete("raw-token");
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(key.capture());
        assertThat(key.getValue()).startsWith("session:").doesNotContain("raw-token");
    }

    @Test
    void mapsSerializationFailuresToBusinessError() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(mock(JsonProcessingException.class));
        TokenService failingService = new TokenService(
                redisTemplate, failingMapper, "session:", Duration.ofHours(8));

        assertThatThrownBy(() -> failingService.create(session()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void returnsEmptyWhenRedisHasNoSession() {
        when(valueOperations.get(anyString())).thenReturn(null);
        assertThat(service.find("raw-token")).isEmpty();
    }

    @Test
    void mapsUnavailableDigestAlgorithmToBusinessError() {
        TokenService failing = new TokenService(redisTemplate, objectMapper, "session:", Duration.ofHours(8)) {
            @Override
            MessageDigest messageDigest() throws NoSuchAlgorithmException {
                throw new NoSuchAlgorithmException("disabled");
            }
        };
        assertThatThrownBy(() -> failing.find("raw-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    private AuthSession session() {
        return new AuthSession(1L, "admin_1", "管理员", "ROLE_1", 1, "15900000000");
    }
}
