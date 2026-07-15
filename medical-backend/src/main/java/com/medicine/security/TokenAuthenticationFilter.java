/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import com.medicine.auth.service.PermissionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final CookieProperties cookieProperties;
    private final PermissionService permissionService;

    public TokenAuthenticationFilter(TokenService tokenService, CookieProperties cookieProperties) {
        this(tokenService, cookieProperties, null);
    }

    @Autowired
    public TokenAuthenticationFilter(TokenService tokenService, CookieProperties cookieProperties,
                                     PermissionService permissionService) {
        this.tokenService = tokenService;
        this.cookieProperties = cookieProperties;
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenService.find(token).ifPresent(session -> {
                // 优先读会话预计算的 authorities,免去每请求多表 JOIN 查权限;
                // 为 null(旧会话或兼容构造)时回退到按请求查询。
                List<String> authorities = session.getAuthorities();
                if (authorities == null) {
                    authorities = permissionService == null
                            ? Collections.singletonList(session.getRoleName())
                            : permissionService.findAuthorities(session.getUserId());
                }
                if (authorities == null || authorities.isEmpty()) {
                    return;
                }
                List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
                for (String authority : authorities) {
                    if (authority != null && !authority.isBlank()) {
                        grantedAuthorities.add(new SimpleGrantedAuthority(authority));
                    }
                }
                if (grantedAuthorities.isEmpty()) {
                    return;
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        session,
                        token,
                        grantedAuthorities
                );
                authentication.setDetails(request.getRemoteAddr());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 优先从 httpOnly cookie 取 token，回退到 Authorization 头（兼容期）。
     */
    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String cookieName = cookieProperties.getName();
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return TokenService.normalizeAuthorization(request.getHeader("Authorization")).orElse(null);
    }
}
