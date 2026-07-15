import { login, getSession, logoutApi } from '../../api/Login';
import { ElMessage } from 'element-plus';
import router, { constantRoutes } from '../../router/index';
import { getMenu } from '../../utils/routeParse';
import {
  normalizeAccess,
  normalizePermissionCodes,
  normalizeRoles,
  normalizePaths,
  userRoles,
} from '../../utils/permissions';
import {
  setUserInfo,
  setLoggedIn,
  setAccess,
  clearAuth,
} from '../../utils/authStore';

// Vue Router 4 returns a disposer from addRoute(). Keep these callbacks out of
// reactive Vuex state and invoke them whenever an account profile changes.
const dynamicRouteDisposers = [];

export function removeDynamicRoutes() {
  while (dynamicRouteDisposers.length) {
    const dispose = dynamicRouteDisposers.pop();
    if (typeof dispose === 'function') {
      try {
        dispose();
      } catch (_) {
        // A stale disposer must never prevent the remaining callbacks running.
      }
    }
  }
}

function flattenRoutePaths(routes, paths = [], names = []) {
  (Array.isArray(routes) ? routes : []).forEach((route) => {
    if (!route || !route.path) {
      return;
    }
    paths.push(route.path);
    if (route.name) {
      names.push(route.name);
    }
    flattenRoutePaths(route.children, paths, names);
  });
  return { paths: normalizePaths(paths), names: [...new Set(names)] };
}

function routePayload(payload) {
  if (Array.isArray(payload)) {
    return { routes: payload, metadata: payload };
  }
  const value = payload && typeof payload === 'object' ? payload : {};
  return {
    routes: Array.isArray(value.routes) ? value.routes : [],
    metadata: value,
  };
}

function isValidUserInfo(value) {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const knownRoles = new Set(['ADMIN', 'DOCTOR', 'PATIENT', 'ROLE_1', 'ROLE_2', 'ROLE_3']);
  const roles = userRoles(value);
  const numericType = Number(value.utype);
  const validNumericType = Number.isInteger(numericType) && numericType >= 1 && numericType <= 3;
  return validNumericType || roles.some((role) => knownRoles.has(role));
}

async function clearRemoteSession() {
  try {
    await logoutApi();
  } catch (_) {
    // Clearing local state is still required when the server is unreachable.
  }
}

const initialState = {
  // Kept for old components that still read the getter. Authentication uses a
  // httpOnly cookie and never writes a token here.
  token: '',
  userInfo: null,
  permissionCodes: [],
  roles: [],
  allowedRoutePaths: [],
  allowedRouteNames: [],
  menuList: constantRoutes.slice(),
  routesLoaded: false,
};

const mutations = {
  SET_TOKEN(state, payload) {
    state.token = payload || '';
  },
  SET_USER_INFO(state, payload) {
    state.userInfo = payload && typeof payload === 'object' ? payload : null;
    const access = normalizeAccess({
      permissionCodes: state.userInfo && state.userInfo.permissionCodes,
      roles: state.userInfo && state.userInfo.roles,
      userInfo: state.userInfo,
      allowedRoutePaths: state.allowedRoutePaths,
      allowedRouteNames: state.allowedRouteNames,
    });
    state.permissionCodes = access.permissionCodes;
    state.roles = access.roles;
    setAccess({
      permissionCodes: state.permissionCodes,
      roles: state.roles,
      allowedRoutePaths: state.allowedRoutePaths,
      allowedRouteNames: state.allowedRouteNames,
    });
  },
  SET_ACCESS(state, payload = {}) {
    const access = normalizeAccess({
      permissionCodes: payload.permissionCodes,
      roles: payload.roles,
      userInfo: state.userInfo,
      allowedRoutePaths: payload.allowedRoutePaths,
      allowedRouteNames: payload.allowedRouteNames,
    });
    state.permissionCodes = access.permissionCodes;
    state.roles = access.roles;
    state.allowedRoutePaths = access.allowedRoutePaths;
    state.allowedRouteNames = access.allowedRouteNames;
    setAccess(access);
  },
  // Store the complete route tree and install only this account's dynamic
  // routes. A route payload may carry permission metadata from a newer API;
  // old APIs continue to work via the role fallback in normalizeAccess().
  SET_ROUTER_MENULIST(state, payload) {
    removeDynamicRoutes();
    const { routes: dynamicRoutes, metadata } = routePayload(payload);
    const routeAccess = flattenRoutePaths(dynamicRoutes);
    const explicitCodes = Object.prototype.hasOwnProperty.call(metadata, 'permissionCodesProvided')
      ? Boolean(metadata.permissionCodesProvided)
      : Object.prototype.hasOwnProperty.call(metadata, 'permissionCodes');
    const suppliedCodes = explicitCodes
      ? normalizePermissionCodes(metadata.permissionCodes)
      : state.permissionCodes;
    const rolesProvided = Object.prototype.hasOwnProperty.call(metadata, 'rolesProvided')
      ? Boolean(metadata.rolesProvided)
      : (Object.prototype.hasOwnProperty.call(metadata, 'roles')
        && normalizeRoles(metadata.roles).length > 0);
    const suppliedRoles = rolesProvided ? normalizeRoles(metadata.roles) : state.roles;
    const paths = metadata.allowedRoutePaths && metadata.allowedRoutePaths.length
      ? normalizePaths(metadata.allowedRoutePaths)
      : routeAccess.paths;
    const names = Array.isArray(metadata.allowedRouteNames) && metadata.allowedRouteNames.length
      ? [...new Set(metadata.allowedRouteNames)]
      : routeAccess.names;

    state.menuList = constantRoutes.concat(dynamicRoutes);
    state.routesLoaded = true;
    state.allowedRoutePaths = paths;
    state.allowedRouteNames = names;
    const access = normalizeAccess({
      permissionCodes: suppliedCodes,
      roles: suppliedRoles,
      userInfo: state.userInfo,
      allowedRoutePaths: paths,
      allowedRouteNames: names,
    });
    state.permissionCodes = access.permissionCodes;
    state.roles = access.roles;
    setAccess(access);

    dynamicRoutes.forEach((route) => {
      const dispose = router.addRoute(route);
      if (typeof dispose === 'function') {
        dynamicRouteDisposers.push(dispose);
      }
    });
  },
  RESET_AUTH(state) {
    removeDynamicRoutes();
    clearAuth();
    state.token = '';
    state.userInfo = null;
    state.permissionCodes = [];
    state.roles = [];
    state.allowedRoutePaths = [];
    state.allowedRouteNames = [];
    state.menuList = constantRoutes.slice();
    state.routesLoaded = false;
  },
};

const actions = {
  async login({ commit }, loginInfo = {}) {
    // Clear all in-memory state before starting a new profile. If credentials
    // fail, the catch path also asks the server to expire any old cookie.
    commit('RESET_AUTH');
    clearAuth();
    const username = String(loginInfo.username || '').trim();
    let res;
    try {
      res = await login(username, loginInfo.password);
    } catch (error) {
      await clearRemoteSession();
      commit('RESET_AUTH');
      clearAuth();
      throw error;
    }
    if (!res || !res.data || Number(res.data.code) !== 20000) {
      await clearRemoteSession();
      commit('RESET_AUTH');
      clearAuth();
      throw new Error((res && res.data && res.data.message) || '账号或密码错误');
    }

    const data = res.data.data || null;
    if (!isValidUserInfo(data)) {
      await clearRemoteSession();
      commit('RESET_AUTH');
      clearAuth();
      throw new Error('登录接口返回的数据不完整');
    }

    setUserInfo(data);
    setLoggedIn(true);
    commit('SET_USER_INFO', data);
    ElMessage({ type: 'success', message: '登录成功' });
    return data;
  },

  async setMenuList({ commit, state }) {
    if (state.routesLoaded) {
      return state.menuList.slice(constantRoutes.length);
    }
    const routes = await getMenu();
    commit('SET_ROUTER_MENULIST', {
      routes,
      permissionCodes: routes.permissionCodes,
      permissionCodesProvided: routes.permissionCodesProvided,
      roles: routes.roles,
      rolesProvided: routes.rolesProvided,
      allowedRoutePaths: routes.allowedRoutePaths,
      allowedRouteNames: routes.allowedRouteNames,
    });
    return routes;
  },

  async checkSession({ commit }) {
    try {
      const data = await getSession();
      if (data && Number(data.code) === 20000 && isValidUserInfo(data.data)) {
        setUserInfo(data.data);
        setLoggedIn(true);
        commit('SET_USER_INFO', data.data);
        return true;
      }
    } catch (_) {
      // Network errors are treated as an expired session below.
    }
    await clearRemoteSession();
    clearAuth();
    commit('RESET_AUTH');
    return false;
  },

  async logout({ commit }) {
    await clearRemoteSession();
    clearAuth();
    commit('RESET_AUTH');
  },
};

export { initialState };

export default {
  namespaced: true,
  state: initialState,
  mutations,
  actions,
};
