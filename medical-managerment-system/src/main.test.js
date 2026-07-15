// @vitest-environment jsdom

import { describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => {
  const app = {
    use: vi.fn(), mixin: vi.fn(), mount: vi.fn(),
    config: { globalProperties: {} },
  };
  return { app, createApp: vi.fn(() => app), attachStore: vi.fn(), user: null };
});

vi.mock('vue', async (importOriginal) => ({ ...(await importOriginal()), createApp: harness.createApp }));
vi.mock('./router', () => ({ default: { name: 'router' }, attachStore: harness.attachStore }));
vi.mock('./store', () => ({ default: { name: 'store' } }));
vi.mock('./utils/authStore', () => ({ getUserInfo: vi.fn(() => harness.user) }));
vi.mock('element-plus', () => ({ default: { name: 'ElementPlus' } }));

describe('application bootstrap', () => {
  it('installs dependencies, permissions, and mounts the app', async () => {
    await import('./main');
    expect(harness.createApp).toHaveBeenCalled();
    expect(harness.attachStore).toHaveBeenCalledWith({ name: 'store' });
    expect(harness.app.use).toHaveBeenCalledTimes(3);
    expect(harness.app.mount).toHaveBeenCalledWith('#app');
    const mixin = harness.app.mixin.mock.calls[0][0];
    harness.user = { utype: 1 };
    expect(mixin.computed.hasRole()).toBe(true);
    harness.user = { utype: 2 };
    expect(mixin.computed.hasRole()).toBe(false);
    harness.user = null;
    expect(mixin.computed.hasRole()).toBe(false);
  });
});
