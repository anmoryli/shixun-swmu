import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  request: vi.fn(),
  resolveApiUrl: vi.fn((path) => `/api/${path}`),
}));

vi.mock('../utils/request', () => ({
  default: mocks.request,
  resolveApiUrl: mocks.resolveApiUrl,
}));

import { getDashboard } from './dashboard';
import { getMenuList, getSession, login, logoutApi } from './Login';

describe('public API request contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.request.mockResolvedValue({ data: { code: 20000 } });
  });

  afterEach(() => vi.unstubAllGlobals());

  it('sends form-encoded login credentials', async () => {
    await login('admin_1', 'a password');
    expect(mocks.request).toHaveBeenCalledWith({
      url: '/login',
      method: 'POST',
      data: 'username=admin_1&password=a%20password',
    });
  });

  it('uses the expected menu, logout, and dashboard contracts', async () => {
    await getMenuList('ROLE_1');
    expect(mocks.request).toHaveBeenLastCalledWith({
      url: '/permissions', method: 'GET',
    });

    await logoutApi();
    expect(mocks.request).toHaveBeenLastCalledWith({ url: '/logout', method: 'POST' });

    await getDashboard();
    expect(mocks.request).toHaveBeenLastCalledWith({ url: '/dashboard', method: 'GET' });
  });

  it('restores a session with same-origin credentials', async () => {
    const json = { code: 20000, data: { id: 1 } };
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue(json) });
    vi.stubGlobal('fetch', fetchMock);

    await expect(getSession()).resolves.toEqual(json);
    expect(fetchMock).toHaveBeenCalledWith('/api/session', {
      method: 'GET', credentials: 'include', headers: { Accept: 'application/json' },
    });
  });

  it('normalizes a failed session probe to token-expired response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false }));

    await expect(getSession()).resolves.toEqual({
      code: 10006, message: '会话探测失败', data: null,
    });
  });
});
