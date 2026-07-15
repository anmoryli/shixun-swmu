package com.medicine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicine.auth.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityComponentsTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenFilterAuthenticatesCookieAndAuthorizationTokens() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        CookieProperties properties = new CookieProperties();
        properties.setName("medicine_token");
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokenService, properties);
        AuthSession session = new AuthSession(1L, "admin", "Admin", "ROLE_1", 1, "15900000000");
        when(tokenService.find("cookie-token")).thenReturn(Optional.of(session));

        MockHttpServletRequest cookieRequest = new MockHttpServletRequest();
        cookieRequest.setRemoteAddr("127.0.0.1");
        cookieRequest.setCookies(new Cookie("other", "x"), new Cookie("medicine_token", "cookie-token"));
        filter.doFilterInternal(cookieRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertSame(session, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals("127.0.0.1", SecurityContextHolder.getContext().getAuthentication().getDetails());

        SecurityContextHolder.clearContext();
        when(tokenService.find("header-token")).thenReturn(Optional.empty());
        MockHttpServletRequest headerRequest = new MockHttpServletRequest();
        headerRequest.setCookies(new Cookie("medicine_token", ""));
        headerRequest.addHeader("Authorization", "Bearer header-token");
        filter.doFilterInternal(headerRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService).find("header-token");
    }

    @Test
    void tokenFilterSkipsLookupWhenAlreadyAuthenticatedOrTokenMissing() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokenService, new CookieProperties());
        MockHttpServletRequest empty = new MockHttpServletRequest();
        filter.doFilterInternal(empty, new MockHttpServletResponse(), new MockFilterChain());
        verifyNoInteractions(tokenService);

        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user", "token"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("medicine_token", "present"));
        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        verifyNoInteractions(tokenService);
    }

    @Test
    void tokenFilterRejectsSessionsWithoutUsableDatabaseAuthorities() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        PermissionService permissionService = mock(PermissionService.class);
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
                tokenService, new CookieProperties(), permissionService);
        AuthSession session = new AuthSession(1L, "admin", "Admin", "ROLE_1", 1, "15900000000");
        when(tokenService.find("token")).thenReturn(Optional.of(session));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        when(permissionService.findAuthorities(1L)).thenReturn(Collections.emptyList());
        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        when(permissionService.findAuthorities(1L)).thenReturn(Arrays.asList("", null));
        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void restSecurityHandlersWriteLegacyHttp200JsonResponses() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MockHttpServletResponse denied = new MockHttpServletResponse();
        new RestAccessDeniedHandler(mapper).handle(new MockHttpServletRequest(), denied,
                new AccessDeniedException("denied"));
        assertEquals(200, denied.getStatus());
        assertTrue(denied.getContentType().startsWith("application/json"));
        assertTrue(denied.getContentAsString().contains("10003"));

        MockHttpServletResponse unauthenticated = new MockHttpServletResponse();
        new RestAuthenticationEntryPoint(mapper).commence(new MockHttpServletRequest(), unauthenticated,
                new BadCredentialsException("bad"));
        assertEquals(200, unauthenticated.getStatus());
        assertTrue(unauthenticated.getContentAsString().contains("10006"));
    }
}
