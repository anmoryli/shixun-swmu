import { beforeEach, describe, expect, it, vi } from 'vitest';

const axiosHarness = vi.hoisted(() => {
  const requestHandlers = [];
  const responseHandlers = [];
  const service = {
    interceptors: {
      request: { use: vi.fn((ok, fail) => requestHandlers.push([ok, fail])) },
      response: { use: vi.fn((ok, fail) => responseHandlers.push([ok, fail])) },
    },
  };
  return { requestHandlers, responseHandlers, service, create: vi.fn(() => service) };
});
const routerHarness = vi.hoisted(() => ({
  router: { currentRoute: { value: { path: '/home' } }, replace: vi.fn() },
}));
const authHarness = vi.hoisted(() => ({ clearAuth: vi.fn(), user: { utype: 1 } }));
const loginHarness = vi.hoisted(() => ({ getMenuList: vi.fn() }));

vi.mock('axios', () => ({ default: { create: axiosHarness.create } }));
vi.mock('element-plus', () => ({ ElMessage: vi.fn() }));
vi.mock('../router/index', () => ({ default: routerHarness.router }));
vi.mock('./authStore', () => ({
  clearAuth: authHarness.clearAuth,
  getUserInfo: vi.fn(() => authHarness.user),
}));
vi.mock('../api/Login', () => ({ getMenuList: loginHarness.getMenuList }));

import { API_BASE_URL, resolveApiUrl } from './request';
import { getMenu } from './routeParse';

describe('request infrastructure', () => {
  beforeEach(() => vi.clearAllMocks());

  it('builds URLs and executes request interceptor paths', async () => {
    expect(API_BASE_URL).toBe('/api');
    expect(resolveApiUrl('/files/a.png')).toBe('/api/files/a.png');
    expect(axiosHarness.requestHandlers).toHaveLength(1);
    const [success, failure] = axiosHarness.requestHandlers[0];
    const config = { url: '/health' };
    expect(success(config)).toBe(config);
    await expect(failure(new Error('request failed'))).rejects.toThrow('request failed');
  });

  it('handles normal, expired, and failed responses', async () => {
    vi.useFakeTimers();
    const [success, failure] = axiosHarness.responseHandlers[0];
    const normal = { data: { code: 20000 } };
    expect(success(normal)).toBe(normal);

    const expired = { data: { code: 10006 } };
    expect(success(expired)).toBe(expired);
    await vi.runAllTimersAsync();
    expect(authHarness.clearAuth).toHaveBeenCalled();
    expect(routerHarness.router.replace).toHaveBeenCalledWith('/user/login');

    routerHarness.router.currentRoute.value.path = '/user/login';
    success(expired);
    await vi.runAllTimersAsync();
    expect(routerHarness.router.replace).toHaveBeenCalledTimes(1);
    await expect(failure(new Error('response failed'))).rejects.toThrow('response failed');
    vi.useRealTimers();
  });
});

describe('dynamic menu parsing', () => {
  beforeEach(() => {
    authHarness.user = { utype: 1 };
    loginHarness.getMenuList.mockReset();
  });

  it('rejects missing identities and unsuccessful API results', async () => {
    authHarness.user = null;
    await expect(getMenu()).rejects.toThrow();

    authHarness.user = { utype: 2 };
    loginHarness.getMenuList.mockResolvedValueOnce({ data: { code: 500, message: 'denied' } });
    await expect(getMenu()).rejects.toThrow('denied');
    loginHarness.getMenuList.mockResolvedValueOnce({ data: { code: 500 } });
    await expect(getMenu()).rejects.toThrow();
  });

  it('filters invalid routes and creates nested Layout/view routes', async () => {
    loginHarness.getMenuList.mockResolvedValueOnce({ data: { code: 20000, data: { permissions: null } } });
    await expect(getMenu()).rejects.toThrow();

    loginHarness.getMenuList.mockResolvedValueOnce({
      data: {
        code: 20000,
        data: {
          permissions: [
            null,
            { path: '', component: 'Layout' },
            { path: '/missing', component: 'MissingView' },
            {
              path: '/', component: 'Layout', meta: { title: 'Root' },
              children: [{ path: '/city', component: 'CityManage', title: 'City' }],
            },
          ],
        },
      },
    });
    const routes = await getMenu();
    expect(routes).toHaveLength(1);
    expect(loginHarness.getMenuList).toHaveBeenCalledWith();
    expect(routes[0]).toMatchObject({ path: '/', name: '/', meta: { title: 'Root' } });
    expect(routes[0].children[0]).toMatchObject({ path: '/city', meta: { title: 'City' } });
  });

  it('preserves explicit access metadata and accepts role-only identities', async () => {
    authHarness.user = { roles: ['DOCTOR'] };
    loginHarness.getMenuList.mockResolvedValueOnce({
      data: {
        code: 20000,
        data: {
          permissions: [{ path: '/home', component: 'Home', hidden: false }],
          permissionCodes: ['dashboard:read'],
          roles: ['doctor'],
        },
      },
    });
    const routes = await getMenu();
    expect(routes.permissionCodes).toEqual(['dashboard:read']);
    expect(routes.permissionCodesProvided).toBe(true);
    expect(routes.roles).toEqual(['DOCTOR']);
    expect(routes.rolesProvided).toBe(true);
    expect(routes.allowedRoutePaths).toEqual(['/home']);
  });
});
