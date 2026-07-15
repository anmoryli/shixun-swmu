// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import store from './index';

describe('Vuex store assembly', () => {
  it('loads every module through the eager module glob', () => {
    expect(store).toBeTruthy();
    expect(Object.keys(store._modulesNamespaceMap).length).toBeGreaterThan(0);
    expect(store.state).toHaveProperty('app');
    expect(store.getters).toBeTruthy();
  });
});
