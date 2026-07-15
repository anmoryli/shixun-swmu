/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import request, { resolveApiUrl } from '../utils/request';
import Qs from 'qs';

export function login(username, password) {
    return request({
        url: '/login',
        method: 'POST',
        data: Qs.stringify({ username, password }),
    });
}
// The server derives the role from the authenticated httpOnly-cookie session.
// Keep an ignored argument for source compatibility with older callers, but
// never send a client-selected role to the authorization endpoint.
export function getMenuList(_legacyRoleName) {
    return request({
        url: '/permissions',
        method: 'GET',
    });
}
export function logoutApi() {
    return request({
        url: '/logout',
        method: 'POST',
    });
}

// 会话探测：用 fetch 绕过通用响应拦截器，未登录(10006)时不触发"登录已失效"弹窗。
// httpOnly cookie 由浏览器自动携带（credentials: include）。
export async function getSession() {
    const resp = await fetch(resolveApiUrl('session'), {
        method: 'GET',
        credentials: 'include',
        headers: { Accept: 'application/json' },
    });
    if (!resp.ok) {
        return { code: 10006, message: '会话探测失败', data: null };
    }
    return resp.json();
}
