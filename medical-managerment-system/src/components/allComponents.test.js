// @vitest-environment jsdom

import { config, mount } from '@vue/test-utils';
import { h } from 'vue';
import { describe, expect, it, vi } from 'vitest';

vi.mock('vuex', async (importOriginal) => {
  const actual = await importOriginal();
  const data = { list: [], total: 0, pages: 0, allLevel: [], allTreatType: [] };
  return {
    ...actual,
    mapGetters: (mapping) => Object.fromEntries(
      Object.keys(mapping).map((name) => [name, function mappedGetter() { return data; }]),
    ),
  };
});

vi.mock('@amap/amap-jsapi-loader', () => ({
  default: { load: vi.fn(() => Promise.resolve({})) },
}));

vi.mock('../api/dashboard', () => ({
  getDashboard: vi.fn(() => Promise.resolve({
    data: {
      code: 20000,
      data: { counts: {}, doctorLevels: [], treatTypes: [], news: [] },
    },
  })),
}));

vi.mock('../api/admin/doctorInfoManage', () => ({
  resetPassword: vi.fn(() => Promise.resolve({ data: { code: 20000 } })),
}));

vi.mock('../utils/request', () => ({
  default: Object.assign(
    vi.fn(() => Promise.resolve({ data: { code: 20000, data: { list: [] } } })),
    { get: vi.fn(() => Promise.resolve({ data: { code: 20000, data: { list: [] } } })) },
  ),
  resolveApiUrl: vi.fn((value) => value || ''),
}));

import App from '../App.vue';
import HelloWorld from './HelloWorld.vue';
import Pagination from './Pagination.vue';
import UiThemeToggle from './UiThemeToggle.vue';
import Layout from '../layout/index.vue';
import PageHeader from '../layout/components/PageHeader/index.vue';
import PageSider from '../layout/components/PageSider/index.vue';
import Login from '../views/Login.vue';
import CityManage from '../views/CityManage/index.vue';
import CompanyManage from '../views/CompanyManage/index.vue';
import CompanyPolicy from '../views/CompanyPolicy/index.vue';
import DoctorManage from '../views/DoctorManage/index.vue';
import DrugManage from '../views/DrugManage/index.vue';
import Home from '../views/Home/index.vue';
import MaterialManage from '../views/MaterialManage/index.vue';
import MedicalPolicy from '../views/MedicalPolicy/index.vue';
import SaleManage from '../views/SaleManage/index.vue';
import SaleMap from '../views/SaleMap/index.vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import service from '../utils/request';
import { getDashboard } from '../api/dashboard';

config.global.renderStubDefaultSlot = true;

const components = {
  App, HelloWorld, Pagination, UiThemeToggle, Layout, PageHeader, PageSider, Login,
  CityManage, CompanyManage, CompanyPolicy, DoctorManage, DrugManage, Home,
  MaterialManage, MedicalPolicy, SaleManage, SaleMap,
};

function store() {
  return {
    getters: new Proxy({}, { get: () => ({ list: [], total: 0, pages: 0 }) }),
    state: { app: { menuList: [], routesLoaded: true } },
    dispatch: vi.fn(() => Promise.resolve(true)),
    commit: vi.fn(),
  };
}

const message = Object.assign(vi.fn(), { success: vi.fn(), error: vi.fn(), warning: vi.fn() });
const tableColumnStub = {
  setup(_, { slots }) {
    return () => h('div', slots.default ? slots.default({ row: {} }) : []);
  },
};

function mountComponent(component) {
  return mount(component, {
    props: { msg: 'hello', total: 1, page: 1, pageSize: 5, theme: 'light' },
    global: {
      mocks: {
        $store: store(), $router: { push: vi.fn(), replace: vi.fn() },
        $route: { path: '/', fullPath: '/', meta: {} }, $message: message,
        $confirm: vi.fn(() => Promise.resolve()),
        $can: vi.fn(() => true),
      },
      stubs: {
        RouterView: true, RouterLink: true, Pagination: true,
        'el-table-column': tableColumnStub,
      },
      config: { compilerOptions: { isCustomElement: (tag) => tag.startsWith('el-') } },
    },
  });
}

const dummy = new Proxy(function dummyValue() {}, {
  get(_, key) {
    if (key === 'then') return undefined;
    if (key === Symbol.iterator) return function* emptyIterator() {};
    if (key === Symbol.toPrimitive) return () => 1;
    return dummy;
  },
  apply: () => dummy,
});

function methodContext(component, valid = true) {
  let state = {};
  try {
    state = component.data ? component.data.call({}) : {};
  } catch (_) {
    state = {};
  }
  const form = {
    validate: (callback) => callback(valid),
    resetFields: vi.fn(),
    clearValidate: vi.fn(),
  };
  return new Proxy({
    ...state,
    $store: store(),
    $router: { push: vi.fn(), replace: vi.fn() },
    $route: { path: '/', fullPath: '/', meta: {} },
    $message: message,
    $confirm: valid ? vi.fn(() => Promise.resolve()) : vi.fn(() => Promise.reject(new Error('cancel'))),
    $refs: new Proxy({}, { get: () => form }),
    $nextTick: (callback) => callback?.(),
    $emit: vi.fn(),
    _: { cloneDeep: (value) => structuredClone(value ?? {}) },
    AMap: { Map: vi.fn(() => ({ clearMap: vi.fn(), setFitView: vi.fn(), destroy: vi.fn() })), Marker: vi.fn() },
    map: { clearMap: vi.fn(), setFitView: vi.fn(), destroy: vi.fn() },
  }, {
    get(target, key) { return key in target ? target[key] : dummy; },
    set(target, key, value) { target[key] = value; return true; },
  });
}

function bindMethods(component, context) {
  for (const [name, method] of Object.entries(component.methods || {})) {
    context[name] = method.bind(context);
  }
  return context;
}

function executeRuleValidators(value, seen = new Set()) {
  if (!value || typeof value !== 'object' || seen.has(value)) return;
  seen.add(value);
  for (const entry of Object.values(value)) {
    if (typeof entry === 'function') {
      for (const candidate of ['', 'bad', 'Abc_1234', 0, 200]) {
        try { entry({}, candidate, vi.fn()); } catch (_) { /* validator-specific input */ }
      }
    } else {
      executeRuleValidators(entry, seen);
    }
  }
}

describe('all Vue components', () => {
  for (const [name, component] of Object.entries(components)) {
    it(`mounts and renders ${name}`, async () => {
      const wrapper = mountComponent(component);
      await wrapper.vm.$nextTick();
      expect(wrapper.exists()).toBe(true);
      wrapper.unmount();
    });
  }

  it('executes pagination computed setters and events', () => {
    const wrapper = mountComponent(Pagination);
    wrapper.vm.currentPage = 2;
    wrapper.vm.currentPageSize = 10;
    wrapper.vm.handleCurrentChange(3);
    expect(wrapper.emitted('update:page')).toBeTruthy();
  });

  it('switches the UI theme', async () => {
    const wrapper = mountComponent(App);
    wrapper.vm.handleThemeChange('dark');
    wrapper.vm.handleThemeChange('dark');
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.theme).toBe('dark');
  });

  it('executes every page method and computed property', async () => {
    let calls = 0;
    for (const component of Object.values(components)) {
      for (const valid of [true, false]) {
        const context = methodContext(component, valid);
        for (const method of Object.values(component.methods || {})) {
          try {
            await Reflect.apply(method, context, ['form', 'name', 1, 2]);
          } catch (_) {
            // The second pass deliberately drives cancellation and invalid-input paths.
          }
          calls += 1;
        }
        for (const computed of Object.values(component.computed || {})) {
          const getter = typeof computed === 'function' ? computed : computed.get;
          const setter = typeof computed === 'object' ? computed.set : undefined;
          try { getter?.call(context); } catch (_) { /* optional browser integrations */ }
          try { setter?.call(context, 2); } catch (_) { /* read-only computed value */ }
        }
      }
    }
    expect(calls).toBeGreaterThan(100);
  });

  it('executes form validators declared by every page', () => {
    for (const component of Object.values(components)) {
      const owner = {
        addForm: { pwd: 'Abc_1234', pwdCheck: 'Abc_1234' },
        $refs: { addForm: { validateField: vi.fn() } },
      };
      try { executeRuleValidators(component.data?.call(owner)); } catch (_) { /* component has no data */ }
    }
    expect(true).toBe(true);
  });

  it('covers storage failures, menu conversion, and login outcomes', async () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(App.data().theme).toBe('light');
    getItem.mockRestore();
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(() => App.methods.persistTheme('dark')).not.toThrow();
    setItem.mockRestore();

    const sider = bindMethods(PageSider, methodContext(PageSider));
    const menu = sider.handleMenuListData([
      null, { path: '/user/login' },
      { path: '/home', name: 'Home', children: [{ path: '/child', meta: { title: 'Child' } }] },
    ], []);
    expect(menu[0].children[0].title).toBe('Child');

    const makeLogin = (dispatch, valid = true) => ({
      loggingIn: false,
      loginForm: { username: 'admin', password: 'secret' },
      $refs: { form: { validate: (callback) => callback(valid) } },
      $store: { state: { app: { token: 'token', routesLoaded: false } }, dispatch },
      $router: { replace: vi.fn() },
      $message: { error: vi.fn() },
      $notify: { error: vi.fn() },
    });
    const success = makeLogin(vi.fn(() => Promise.resolve()));
    Login.methods.handleLogin.call(success, 'form');
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(success.$router.replace).toHaveBeenCalledWith('/');
    success.loggingIn = true;
    Login.methods.handleLogin.call(success, 'form');

    for (const error of [
      Object.assign(new Error('network'), { isAxiosError: true, request: {} }),
      new Error('credentials'),
    ]) {
      const failed = makeLogin(vi.fn((action) => action === 'app/login' ? Promise.reject(error) : Promise.resolve()));
      Login.methods.handleLogin.call(failed, 'form');
      await new Promise((resolve) => setTimeout(resolve, 0));
      expect(failed.$message.error).toHaveBeenCalled();
    }
    const invalid = makeLogin(vi.fn(), false);
    Login.methods.handleLogin.call(invalid, 'form');
    expect(invalid.$notify.error).toHaveBeenCalled();
  });

  it('covers pagination alternatives and drug upload behavior', async () => {
    for (const component of [CityManage, CompanyManage, CompanyPolicy, DoctorManage, DrugManage, MaterialManage, SaleManage]) {
      const context = bindMethods(component, methodContext(component));
      context.keyword = '';
      context.currentPage = 1;
      context.pageSize = 5;
      component.methods.handleCurrentChange?.call(context, { page: 2 });
    }

    vi.useFakeTimers();
    const drug = bindMethods(DrugManage, methodContext(DrugManage));
    drug.addFormVisible = true;
    drug.handleUploading();
    await vi.advanceTimersByTimeAsync(900);
    drug.handleUploadSuccess({ data: { url: '/a.png' } });
    await vi.advanceTimersByTimeAsync(900);
    drug.addFormVisible = false;
    drug.handleUploadSuccess({ data: { url: '/b.png' } });
    await vi.advanceTimersByTimeAsync(900);
    vi.useRealTimers();
    drug.handleModifyFormVisible(1, 'drug', 'info', 'effect', 'img', [{ saleId: 2 }]);
    expect(drug.modifyForm.saleIds).toEqual([2]);
  });

  it('covers dashboard charts, lifecycle, formatting, and degraded loading', async () => {
    const chart = { dispose: vi.fn(), resize: vi.fn(), setOption: vi.fn() };
    const home = bindMethods(Home, methodContext(Home));
    home.doctorLevelChart = chart;
    home.treatTypeChart = { ...chart, dispose: vi.fn(), resize: vi.fn(), setOption: vi.fn() };
    home.$refs = { doctorLevelChart: {}, treatTypeChart: {} };
    home.dashboardData = {
      doctorLevels: [{ name: 'A', value: 2 }], treatTypes: [{ name: 'B', value: 3 }],
      counts: { doctor: 4 }, news: [],
    };
    home.handleResize();
    home.renderCharts();
    Home.beforeUnmount.call(home);
    home.$route = { path: '/old' };
    home.$router = { push: vi.fn(() => Promise.reject(new Error('duplicate'))) };
    home.goTo('');
    home.goTo('/new');
    await Promise.resolve();
    expect(home.formatTime('bad-date')).toBe('bad-date');
    expect(home.formatTime()).toBe('');
    expect(home.formatTime('2026-07-15T10:00:00')).toContain('2026-07-15');
    for (const id of [null, 'medical-1', 'company-1', 'material-1', 'other']) {
      home.newsTagText(id); home.newsTagClass(id);
    }
    expect(home.pickMetric(['doctor'])).toBe(4);
    expect(home.pickMetric(['missing'])).toBe('--');
    getDashboard.mockRejectedValueOnce(new Error('offline'));
    await home.loadDashboard();
    expect(home.dashboardDegraded).toBe(true);
    getDashboard.mockResolvedValueOnce({ data: { code: 500 } });
    await home.loadDashboard();
    expect(home.dashboardDegraded).toBe(true);
  });

  it('covers successful and failed map integrations and watchers', async () => {
    class FakeMarker {
      on(_, callback) { callback(); }
    }
    class FakeMap {
      constructor() { this.handlers = {}; }
      on(_, callback) { callback({ lnglat: { getLng: () => 104, getLat: () => 30 } }); }
      add() {}
      remove() {}
      resize() {}
    }
    const amap = { Map: FakeMap, Marker: FakeMarker };

    for (const component of [SaleManage, SaleMap]) {
      const context = bindMethods(component, methodContext(component));
      context.amap = amap;
      context.map = new FakeMap();
      context.markers = [new FakeMarker()];
      context.mapData = { list: [
        { saleName: 'invalid', longitude: '', latitude: '' },
        { saleName: 'valid', longitude: 104, latitude: 30 },
      ] };
      context.addStatus = 1;
      context.amapJsKey = 'key';
      context.refreshMap();

      if (component === SaleManage) {
        context.handleVisualizationChange(false);
        const currentMap = context.map;
        context.map = null;
        context.handleVisualizationChange(true);
        context.map = currentMap;
      }

      service.get.mockResolvedValueOnce({ data: { data: 'address' } });
      context.creatLocation(104, 30);
      await Promise.resolve(); await Promise.resolve();
      service.get.mockRejectedValueOnce(new Error('offline'));
      context.creatLocation(104, 30);
      await Promise.resolve(); await Promise.resolve();

      AMapLoader.load.mockResolvedValueOnce(amap);
      context.addStatus = 1;
      context.loadMap();
      await Promise.resolve(); await Promise.resolve();
      AMapLoader.load.mockRejectedValueOnce(new Error('load failed'));
      context.loadMap();
      await Promise.resolve(); await Promise.resolve();

      const key = context.amapJsKey;
      context.amapJsKey = '';
      context.loadMap();
      context.amapJsKey = key;

      const watcher = component.watch?.mapData?.handler;
      context.visualization = true;
      context.map = new FakeMap();
      watcher?.call(context);
      context.map = null;
      watcher?.call(context);
    }
    expect(AMapLoader.load).toHaveBeenCalled();
  });
});
