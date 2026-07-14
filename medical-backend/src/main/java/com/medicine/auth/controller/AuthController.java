/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.auth.controller;

import com.medicine.auth.dto.LoginRequest;
import com.medicine.auth.dto.LoginResult;
import com.medicine.auth.dto.PermissionNode;
import com.medicine.auth.service.AuthService;
import com.medicine.auth.service.PermissionService;
import com.medicine.common.ApiResponse;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.AuthSession;
import com.medicine.security.CookieProperties;
import com.medicine.security.TokenService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final PermissionService permissionService;
    private final TokenService tokenService;
    private final CookieProperties cookieProperties;

    public AuthController(AuthService authService, PermissionService permissionService, TokenService tokenService,
                          CookieProperties cookieProperties) {
        this.authService = authService;
        this.permissionService = permissionService;
        this.tokenService = tokenService;
        this.cookieProperties = cookieProperties;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<LoginResult> loginForm(@Valid LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request.getUsername(), request.getPassword());
        addAuthCookie(response, result.getToken());
        return ApiResponse.success(result);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LoginResult> loginJson(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request.getUsername(), request.getPassword());
        addAuthCookie(response, result.getToken());
        return ApiResponse.success(result);
    }

    @GetMapping("/permissions")
    public ApiResponse<Map<String, List<PermissionNode>>> permissions(
            @RequestParam(value = "roleName", required = false) String ignoredRoleName,
            Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthSession)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无效的登录会话");
        }
        AuthSession session = (AuthSession) principal;
        List<PermissionNode> permissions = permissionService.findPermissionTree(session.getRoleName());
        return ApiResponse.success(Collections.singletonMap("permissions", permissions));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        tokenService.delete(TokenService.normalizeAuthorization(authorization).orElse(null));
        return ApiResponse.success();
    }

    /**
     * 将不透明 token 写入 httpOnly cookie，由浏览器自动携带、前端 JS 读不到，实现持久登录。
     */
    private void addAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.getName(), token)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .maxAge(cookieProperties.getMaxAge());
        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isEmpty()) {
            builder.domain(cookieProperties.getDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
