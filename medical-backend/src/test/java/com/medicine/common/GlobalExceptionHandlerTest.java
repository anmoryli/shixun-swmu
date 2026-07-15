/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import javax.validation.ConstraintViolationException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void preservesBusinessErrorCodeAndMessage() {
        ApiResponse<Void> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.NOT_FOUND, "记录不存在"));

        assertThat(response.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo("记录不存在");
        assertThat(response.getData()).isNull();
    }

    @Test
    void normalizesUnreadableRequests() {
        ApiResponse<Void> response = handler.handleInvalidRequest(
                new ConstraintViolationException("bad request", java.util.Collections.emptySet()));

        assertThat(response.getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
        assertThat(response.getMessage()).isEqualTo("请求参数不合法");
    }

    @Test
    void normalizesAccessDeniedErrors() {
        ApiResponse<Void> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(response.getMessage()).isEqualTo("无权执行此操作");
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        ApiResponse<Void> response = handler.handleUnexpected(new IllegalStateException("sensitive implementation detail"));

        assertThat(response.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(response.getMessage()).isEqualTo("服务器内部错误");
        assertThat(response.getMessage()).doesNotContain("sensitive");
    }
}
