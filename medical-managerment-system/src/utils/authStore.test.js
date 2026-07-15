import { beforeEach, describe, expect, it } from 'vitest';

import {
  clearAuth,
  getUserInfo,
  isLoggedIn,
  setLoggedIn,
  setUserInfo,
} from './authStore';

describe('authStore', () => {
  beforeEach(() => clearAuth());

  it('starts from a cleared authentication state', () => {
    expect(isLoggedIn()).toBe(false);
    expect(getUserInfo()).toBeNull();
  });

  it('derives login state from user information', () => {
    const user = { id: 1, uname: 'admin_1', utype: 1 };

    setUserInfo(user);

    expect(getUserInfo()).toBe(user);
    expect(isLoggedIn()).toBe(true);
  });

  it('normalizes explicit login state to boolean', () => {
    setLoggedIn('yes');
    expect(isLoggedIn()).toBe(true);

    setLoggedIn(0);
    expect(isLoggedIn()).toBe(false);
  });

  it('clears both user information and login state', () => {
    setUserInfo({ id: 2 });

    clearAuth();

    expect(getUserInfo()).toBeNull();
    expect(isLoggedIn()).toBe(false);
  });
});
