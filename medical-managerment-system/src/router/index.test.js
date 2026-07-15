// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  guard: null,
  loggedIn: false,
  error: vi.fn(),
  router: {
    beforeEach: vi.fn((callback) => { harness.guard = callback; }),
    appStore: undefined,
  },
}));

vi.mock('vue-router', () => ({
  createWebHashHistory: vi.fn(() => ({})),
  createRouter: vi.fn(() => harness.router),
}));
vi.mock('element-plus', () => ({ ElMessage: { error: harness.error } }));
vi.mock('../utils/authStore', () => ({ isLoggedIn: vi.fn(() => harness.loggedIn) }));

import router, { attachStore, constantRoutes } from './index';

const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('router guard', () => {
  beforeEach(() => {
    harness.loggedIn = false;
    harness.router.appStore = undefined;
    harness.error.mockClear();
  });

  it('redirects the root route based on login state and attaches the store', () => {
    harness.loggedIn = false;
    expect(constantRoutes[1].redirect()).toBe('/user/login');
    harness.loggedIn = true;
    expect(constantRoutes[1].redirect()).toBe('/home');
    const store = { state: {} };
    attachStore(store);
    expect(router.appStore).toBe(store);
  });

  it('handles unauthenticated navigation and cookie session recovery', async () => {
    const next = vi.fn();
    await harness.guard({ path: '/home', meta: {}, fullPath: '/home' }, {}, next);
    expect(next).toHaveBeenCalledWith('/user/login');

    next.mockClear();
    await harness.guard({ path: '/user/login', meta: {}, fullPath: '/user/login' }, {}, next);
    expect(next).toHaveBeenCalledWith();

    const store = {
      state: { app: { routesLoaded: true } },
      dispatch: vi.fn(() => Promise.resolve(true)),
    };
    harness.router.appStore = store;
    next.mockClear();
    await harness.guard({ path: '/user/login', meta: { title: 'Login' }, fullPath: '/user/login' }, {}, next);
    expect(next).toHaveBeenCalledWith('/');
  });

  it('continues with loaded routes and reloads newly added routes', async () => {
    harness.loggedIn = true;
    const next = vi.fn();
    harness.router.appStore = {
      state: { app: { routesLoaded: true, allowedRoutePaths: ['/home'] } },
      dispatch: vi.fn(),
    };
    await harness.guard({ path: '/home', meta: {}, fullPath: '/home?a=1' }, {}, next);
    expect(next).toHaveBeenCalledWith();

    const store = {
      state: { app: { routesLoaded: false } },
      dispatch: vi.fn(() => Promise.resolve()),
    };
    harness.router.appStore = store;
    next.mockClear();
    await harness.guard({ path: '/home', meta: {}, fullPath: '/home?a=1' }, {}, next);
    await flush();
    expect(next).toHaveBeenCalledWith({ path: '/home?a=1', replace: true });

    next.mockClear();
    await harness.guard({ path: '/user/login', meta: {}, fullPath: '/user/login' }, {}, next);
    await flush();
    expect(next).toHaveBeenCalledWith('/');
  });

  it('logs out and redirects when dynamic route loading fails', async () => {
    harness.loggedIn = true;
    const next = vi.fn();
    const store = {
      state: { app: { routesLoaded: false } },
      dispatch: vi.fn((action) => action === 'app/setMenuList'
        ? Promise.reject(new Error('menu failed'))
        : Promise.resolve()),
    };
    harness.router.appStore = store;
    await harness.guard({ path: '/home', meta: {}, fullPath: '/home' }, {}, next);
    await flush();
    expect(store.dispatch).toHaveBeenCalledWith('app/logout');
    expect(harness.error).toHaveBeenCalledWith('menu failed');
    expect(next).toHaveBeenCalledWith('/user/login');
  });
});
