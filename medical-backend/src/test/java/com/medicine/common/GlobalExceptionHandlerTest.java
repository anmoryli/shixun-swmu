/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.NOT_FOUND, "记录不存在"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("记录不存在");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void mapsBusinessErrorCodeToHttpStatus() {
        assertThat(handler.handleBusinessException(
                new BusinessException(ErrorCode.INVALID_ARGUMENT, "x")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleBusinessException(
                new BusinessException(ErrorCode.FORBIDDEN, "x")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleBusinessException(
                new BusinessException(ErrorCode.TOKEN_EXPIRED, "x")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleBusinessException(
                new BusinessException(ErrorCode.INTERNAL_ERROR, "x")).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void normalizesUnreadableRequests() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidRequest(
                new ConstraintViolationException("bad request", java.util.Collections.emptySet()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
        assertThat(response.getBody().getMessage()).isEqualTo("请求参数不合法");
    }

    @Test
    void normalizesAccessDeniedErrors() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("无权执行此操作");
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(new IllegalStateException("sensitive implementation detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("服务器内部错误");
        assertThat(response.getBody().getMessage()).doesNotContain("sensitive");
    }

    @Test
    void normalizesMethodArgumentValidationWithAndWithoutFieldErrors() {
        BeanPropertyBindingResult empty = new BeanPropertyBindingResult(new Form(), "form");
        ResponseEntity<ApiResponse<Void>> emptyResponse = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(mockParameter(), empty));
        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(emptyResponse.getBody().getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);

        BeanPropertyBindingResult invalid = new BeanPropertyBindingResult(new Form(), "form");
        invalid.addError(new FieldError("form", "name", "name required"));
        ResponseEntity<ApiResponse<Void>> invalidResponse = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(mockParameter(), invalid));
        assertThat(invalidResponse.getBody().getMessage()).isEqualTo("name required");
    }

    @Test
    void normalizesBindingValidationWithAndWithoutFieldErrors() {
        BindException empty = new BindException(new Form(), "form");
        assertThat(handler.handleBindException(empty).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleBindException(empty).getBody().getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);

        BindException invalid = new BindException(new Form(), "form");
        invalid.addError(new FieldError("form", "name", "bad name"));
        assertThat(handler.handleBindException(invalid).getBody().getMessage()).isEqualTo("bad name");
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

