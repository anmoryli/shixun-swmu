/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import { getMenuList } from '../api/Login';
import Layout from '../layout/index.vue';
import { getUserInfo } from '../utils/authStore';
import {
  normalizePermissionCodes,
  normalizeRoles,
  normalizePaths,
  userRoles,
} from './permissions';

const viewModules = import.meta.glob('../views/*/index.vue');

function tree(data, arr) {
  if (!Array.isArray(data)) {
    return arr;
  }

  data.forEach((item) => {
    if (!item || !item.path || !item.component) {
      return;
    }

    const viewComponent = item.component === 'Layout'
      ? Layout
      : viewModules[`../views/${item.component}/index.vue`];
    if (!viewComponent) {
      return;
    }

    const sourceMeta = item.meta && typeof item.meta === 'object' ? item.meta : {};
    const route = {
      path: item.path,
      name: item.name || item.path,
      component: viewComponent,
      meta: {
        ...sourceMeta,
        title: sourceMeta.title || item.title || item.name || '慧医数字医疗',
      },
      children: [],
    };

    // Older PermissionNode responses put these flags beside meta. Preserve
    // them so the menu and route guard can fail closed without a DB upgrade.
    if (item.hidden !== undefined && route.meta.hidden === undefined) {
      route.meta.hidden = Boolean(item.hidden);
    }
    if (item.permissionCode && route.meta.permissionCode === undefined) {
      route.meta.permissionCode = item.permissionCode;
    }

    if (Array.isArray(item.children) && item.children.length) {
      route.children = tree(item.children, []);
    }
    arr.push(route);
  });
  return arr;
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
  return { paths: normalizePaths(paths), names };
}

function attachAccessMetadata(routes, data, userInfo) {
  const routeAccess = flattenRoutePaths(routes);
  const permissionCodesProvided = Boolean(
    data
      && Object.prototype.hasOwnProperty.call(data, 'permissionCodes')
      && data.permissionCodes !== undefined
      && data.permissionCodes !== null,
  );
  const rolesProvided = Boolean(
    data
      && Object.prototype.hasOwnProperty.call(data, 'roles')
      && data.roles !== undefined
      && data.roles !== null,
  );
  const permissionCodes = normalizePermissionCodes(
    permissionCodesProvided
      ? data.permissionCodes
      : userInfo && userInfo.permissionCodes,
  );
  const roles = normalizeRoles(
    rolesProvided ? data.roles : userRoles(userInfo),
  );
  // Keep the historical array return type. Non-enumerable properties avoid
  // changing callers that compare the parsed route array directly.
  Object.defineProperties(routes, {
    permissionCodes: { value: permissionCodes, enumerable: false, configurable: true },
    permissionCodesProvided: { value: permissionCodesProvided, enumerable: false, configurable: true },
    roles: { value: roles, enumerable: false, configurable: true },
    rolesProvided: { value: rolesProvided, enumerable: false, configurable: true },
    allowedRoutePaths: { value: routeAccess.paths, enumerable: false, configurable: true },
    allowedRouteNames: { value: routeAccess.names, enumerable: false, configurable: true },
  });
  return routes;
}

export async function getMenu() {
  const userInfo = getUserInfo();
  if (!userInfo
    || ((userInfo.utype === null || userInfo.utype === undefined)
      && !userRoles(userInfo).length)) {
    throw new Error('登录信息无效，请重新登录');
  }

  const res = await getMenuList();
  if (!res.data || Number(res.data.code) !== 20000) {
    throw new Error((res.data && res.data.message) || '获取菜单列表失败');
  }

  const data = res.data.data || {};
  const permissions = data.permissions;
  const routes = tree(permissions, []);
  if (!routes.length) {
    throw new Error('当前账号没有可访问的菜单');
  }
  return attachAccessMetadata(routes, data, userInfo);
}

export { flattenRoutePaths, tree };
