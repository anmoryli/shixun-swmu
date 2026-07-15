/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medicine.auth.dto.LoginRequest;
import com.medicine.auth.dto.LoginResult;
import com.medicine.auth.dto.PermissionNode;
import com.medicine.auth.dto.UserInfo;
import com.medicine.auth.service.AuthService;
import com.medicine.auth.service.PermissionService;
import com.medicine.common.ApiResponse;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.AuthSession;
import com.medicine.security.CookieProperties;
import com.medicine.security.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;
import java.util.List;
import java.util.Map;

class AuthControllerTest {

    private AuthService authService;
    private PermissionService permissionService;
    private TokenService tokenService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        permissionService = mock(PermissionService.class);
        tokenService = mock(TokenService.class);
        CookieProperties cookie = new CookieProperties();
        cookie.setName("medicine_token");
        cookie.setPath("/");
        cookie.setSameSite("Lax");
        cookie.setSecure(true);
        cookie.setMaxAge(Duration.ofHours(8));
        controller = new AuthController(authService, permissionService, tokenService, cookie);
    }

    @Test
    void loginWritesHttpOnlyCookieAndDoesNotExposeTokenInBody() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin_1");
        request.setPassword("secret");
        UserInfo user = new UserInfo(1L, "管理员", "admin_1", "15900000000", 1);
        when(authService.login("admin_1", "secret")).thenReturn(new LoginResult("raw-token", user));
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ApiResponse<UserInfo> response = controller.loginJson(request, servletResponse);

        assertThat(response.getCode()).isEqualTo(ErrorCode.SUCCESS);
        assertThat(response.getData()).isSameAs(user);
        assertThat(response.getData().getUname()).doesNotContain("raw-token");
        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
                .contains("medicine_token=raw-token")
                .contains("Path=/")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Max-Age=28800");
    }

    @Test
    void formLoginAndDomainCookiesAreCovered() {
        CookieProperties cookie = new CookieProperties();
        cookie.setName("medicine_token");
        cookie.setPath("/");
        cookie.setSameSite("Lax");
        cookie.setDomain("example.test");
        cookie.setMaxAge(Duration.ofHours(1));
        AuthController domainController = new AuthController(authService, permissionService, tokenService, cookie);
        LoginRequest request = new LoginRequest();
        request.setUsername("admin_1");
        request.setPassword("secret");
        UserInfo user = new UserInfo(1L, "Admin", "admin_1", "15900000000", 1);
        when(authService.login("admin_1", "secret")).thenReturn(new LoginResult("raw-token", user));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(domainController.loginForm(request, response).getData()).isSameAs(user);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Domain=example.test");

        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();
        domainController.logout(null, logoutResponse);
        verify(tokenService).delete(null);
        assertThat(logoutResponse.getHeader(HttpHeaders.SET_COOKIE)).contains("Domain=example.test");
    }

    @Test
    void permissionsAlwaysUseTheAuthenticatedRole() {
        AuthSession session = session();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(session, "token");
        PermissionNode node = new PermissionNode();
        when(permissionService.findPermissionTree(1L)).thenReturn(List.of(node));

        ApiResponse<Map<String, Object>> response =
                controller.permissions("ROLE_2", authentication);

        assertThat(response.getData().get("permissions")).isEqualTo(List.of(node));
        verify(permissionService).findPermissionTree(1L);
    }

    @Test
    void permissionsRejectInvalidPrincipals() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("anonymous", "token");

        assertThatThrownBy(() -> controller.permissions(null, authentication))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void sessionReturnsUserInfoOnlyForAnAuthenticatedSession() {
        ApiResponse<UserInfo> active = controller.session(
                new UsernamePasswordAuthenticationToken(session(), "token"));
        ApiResponse<UserInfo> missing = controller.session(null);

        assertThat(active.getCode()).isEqualTo(ErrorCode.SUCCESS);
        assertThat(active.getData().getUname()).isEqualTo("admin_1");
        assertThat(active.getData().getUtype()).isEqualTo(1);
        assertThat(missing.getCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED);
        assertThat(missing.getData()).isNull();
    }

    @Test
    void logoutDeletesServerSessionAndExpiresCookie() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(session(), "raw-token");

        ApiResponse<Void> response = controller.logout(authentication, servletResponse);

        assertThat(response.getCode()).isEqualTo(ErrorCode.SUCCESS);
        verify(tokenService).delete("raw-token");
        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
                .contains("medicine_token=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
    }

    private AuthSession session() {
        return new AuthSession(1L, "admin_1", "管理员", "ROLE_1", 1, "15900000000");
    }
}
