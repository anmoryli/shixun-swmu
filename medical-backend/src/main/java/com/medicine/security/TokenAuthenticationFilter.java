/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final CookieProperties cookieProperties;

    public TokenAuthenticationFilter(TokenService tokenService, CookieProperties cookieProperties) {
        this.tokenService = tokenService;
        this.cookieProperties = cookieProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenService.find(token).ifPresent(session -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        session,
                        token,
                        Collections.singletonList(new SimpleGrantedAuthority(session.getRoleName()))
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
