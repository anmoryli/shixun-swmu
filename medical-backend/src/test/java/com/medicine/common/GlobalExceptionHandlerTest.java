/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @Test
    void normalizesMethodArgumentValidationWithAndWithoutFieldErrors() {
        BeanPropertyBindingResult empty = new BeanPropertyBindingResult(new Form(), "form");
        ApiResponse<Void> emptyResponse = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(mockParameter(), empty));
        assertThat(emptyResponse.getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);

        BeanPropertyBindingResult invalid = new BeanPropertyBindingResult(new Form(), "form");
        invalid.addError(new FieldError("form", "name", "name required"));
        ApiResponse<Void> invalidResponse = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(mockParameter(), invalid));
        assertThat(invalidResponse.getMessage()).isEqualTo("name required");
    }

    @Test
    void normalizesBindingValidationWithAndWithoutFieldErrors() {
        BindException empty = new BindException(new Form(), "form");
        assertThat(handler.handleBindException(empty).getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);

        BindException invalid = new BindException(new Form(), "form");
        invalid.addError(new FieldError("form", "name", "bad name"));
        assertThat(handler.handleBindException(invalid).getMessage()).isEqualTo("bad name");
    }

    private MethodParameter mockParameter() {
        return org.mockito.Mockito.mock(MethodParameter.class);
    }

    private static class Form {
        private String name;

        public String getName() {
            return name;
        }
    }
}
