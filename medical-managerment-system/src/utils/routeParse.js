/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import { getMenuList } from '../api/Login';
import Layout from '../layout/index.vue';
import { getUserInfo } from '../utils/authStore';

function tree(data, arr) {
  if (!Array.isArray(data)) {
    return arr;
  }

  data.forEach((item) => {
    if (!item || !item.path || !item.component) {
      return;
    }

    const route = {
      path: item.path,
      name: item.name || item.path,
      component:
        item.component === 'Layout'
          ? Layout
          : () => import(`../views/${item.component}/index.vue`),
      meta: {
        title: (item.meta && item.meta.title) || item.title || item.name || '慧医数字医疗',
      },
      children: [],
    };

    if (Array.isArray(item.children) && item.children.length) {
      route.children = tree(item.children, []);
    }
    arr.push(route);
  });
  return arr;
}

function getStoredRole() {
  const userInfo = getUserInfo();
  return userInfo && userInfo.utype;
}

export async function getMenu() {
  const roleName = getStoredRole();
  if (roleName === null || roleName === undefined) {
    throw new Error('登录信息无效，请重新登录');
  }

  const res = await getMenuList(roleName);
  if (!res.data || Number(res.data.code) !== 20000) {
    throw new Error((res.data && res.data.message) || '获取菜单列表失败');
  }

  const permissions = res.data.data && res.data.data.permissions;
  const routes = tree(permissions, []);
  if (!routes.length) {
    throw new Error('当前账号没有可访问的菜单');
  }
  return routes;
}
