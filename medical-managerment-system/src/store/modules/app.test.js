import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiMocks = vi.hoisted(() => ({
  login: vi.fn(),
  getSession: vi.fn(),
  logoutApi: vi.fn(),
  getMenu: vi.fn(),
  addRoute: vi.fn(),
  message: vi.fn(),
}));

vi.mock('../../api/Login', () => ({
  login: apiMocks.login,
  getSession: apiMocks.getSession,
  logoutApi: apiMocks.logoutApi,
}));

vi.mock('../../utils/routeParse', () => ({ getMenu: apiMocks.getMenu }));
vi.mock('../../router/index', () => ({
  default: { addRoute: apiMocks.addRoute },
  constantRoutes: [{ path: '/user/login' }, { path: '/' }],
}));
vi.mock('element-plus', () => ({ ElMessage: apiMocks.message }));

import { clearAuth, getUserInfo, isLoggedIn } from '../../utils/authStore';
import appModule from './app';

function freshState() {
  return {
    token: '',
    menuList: [{ path: '/user/login' }, { path: '/' }],
    routesLoaded: false,
  };
}

describe('app store module', () => {
  beforeEach(() => {
    clearAuth();
    vi.clearAllMocks();
  });

  it('logs in with a trimmed username and keeps only user information in memory', async () => {
    const user = { id: 1, uname: 'admin_1', utype: 1 };
    apiMocks.login.mockResolvedValue({ data: { code: 20000, data: user } });
    const commit = vi.fn();

    await expect(appModule.actions.login({ commit }, {
      username: ' admin_1 ',
      password: 'secret',
    })).resolves.toBe(user);

    expect(apiMocks.login).toHaveBeenCalledWith('admin_1', 'secret');
    expect(commit).toHaveBeenCalledWith('RESET_AUTH');
    expect(getUserInfo()).toBe(user);
    expect(isLoggedIn()).toBe(true);
    expect(apiMocks.message).toHaveBeenCalledWith(expect.objectContaining({ type: 'success' }));
  });

  it('rejects failed and incomplete login responses', async () => {
    apiMocks.login.mockResolvedValueOnce({ data: { code: 10002, message: '账号或密码错误' } });
    await expect(appModule.actions.login({ commit: vi.fn() }, {
      username: 'admin_1', password: 'wrong',
    })).rejects.toThrow('账号或密码错误');

    apiMocks.login.mockResolvedValueOnce({ data: { code: 20000, data: null } });
    await expect(appModule.actions.login({ commit: vi.fn() }, {
      username: 'admin_1', password: 'secret',
    })).rejects.toThrow('登录接口返回的数据不完整');
  });

  it('restores a valid server session', async () => {
    const user = { id: 2, uname: 'doctor_2', utype: 2 };
    apiMocks.getSession.mockResolvedValue({ code: 20000, data: user });

    await expect(appModule.actions.checkSession({ commit: vi.fn() })).resolves.toBe(true);
    expect(getUserInfo()).toBe(user);
    expect(isLoggedIn()).toBe(true);
  });

  it.each([
    [{ code: 10006, data: null }],
    [new Error('network down')],
  ])('clears authentication when session restoration fails', async (result) => {
    if (result instanceof Error) {
      apiMocks.getSession.mockRejectedValue(result);
    } else {
      apiMocks.getSession.mockResolvedValue(result);
    }
    const commit = vi.fn();

    await expect(appModule.actions.checkSession({ commit })).resolves.toBe(false);
    expect(isLoggedIn()).toBe(false);
    expect(commit).toHaveBeenCalledWith('RESET_AUTH');
  });

  it('loads and registers dynamic routes once', async () => {
    const route = { path: '/home' };
    apiMocks.getMenu.mockResolvedValue([route]);
    const state = freshState();
    const commit = (name, payload) => appModule.mutations[name](state, payload);

    await expect(appModule.actions.setMenuList({ commit, state })).resolves.toEqual([route]);
    await expect(appModule.actions.setMenuList({ commit, state })).resolves.toEqual([route]);

    expect(apiMocks.getMenu).toHaveBeenCalledTimes(1);
    expect(apiMocks.addRoute).toHaveBeenCalledWith(route);
    expect(state.routesLoaded).toBe(true);
  });

  it('clears local authentication even when the logout API fails', async () => {
    apiMocks.logoutApi.mockRejectedValue(new Error('offline'));
    const commit = vi.fn();

    await expect(appModule.actions.logout({ commit })).resolves.toBeUndefined();
    expect(commit).toHaveBeenCalledWith('RESET_AUTH');
    expect(isLoggedIn()).toBe(false);
  });

  it('resets route and token state without sharing the constant route array', () => {
    const state = freshState();
    state.token = 'legacy-token';
    state.routesLoaded = true;
    state.menuList.push({ path: '/home' });

    appModule.mutations.RESET_AUTH(state);

    expect(state).toMatchObject({ token: '', routesLoaded: false });
    expect(state.menuList.map((route) => route.path)).toEqual(['/user/login', '/']);
  });
});
