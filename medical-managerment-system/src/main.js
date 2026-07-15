/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import { createApp } from 'vue';
import App from './App.vue';
import router, { attachStore } from './router'; // 引入router
import store from './store'; // 引入vuex
import { getUserInfo } from './utils/authStore';

import _ from 'lodash'; // js库
import './style/reset.css'; // 引入样式
import 'animate.css'; // 引入样式
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './style/theme.css';
import { can, hasRole } from './utils/permissions';

function accessFor(vm) {
  const appState = vm && vm.$store && vm.$store.state && vm.$store.state.app;
  const fallbackUser = getUserInfo();
  if (!appState) {
    return { userInfo: fallbackUser };
  }
  const access = {
    userInfo: appState.userInfo || fallbackUser,
    roles: appState.roles,
    allowedRoutePaths: appState.allowedRoutePaths,
  };
  // An explicitly empty permission list is meaningful (read-only account),
  // while an omitted field in lightweight test/legacy stores should fall back
  // to the user's role.
  if (Array.isArray(appState.permissionCodes)) {
    access.permissionCodes = appState.permissionCodes;
  }
  return access;
}

const app = createApp(App);
attachStore(store);
app.use(ElementPlus);
app.config.globalProperties._ = _;
app.config.globalProperties.$can = function checkPermission(code) {
  return can(code, accessFor(this));
};
app.config.globalProperties.$canManage = function checkWritePermission() {
  return can(':write', accessFor(this));
};
app.mixin({
  computed: {
    // Capability-first API. `hasRole` remains as a compatibility alias for
    // templates/extensions that have not migrated yet.
    canManage() {
      return can(':write', accessFor(this));
    },
    can() {
      return (code) => can(code, accessFor(this));
    },
    hasRole() {
      const userInfo = (this && this.$store && this.$store.state
        && this.$store.state.app && this.$store.state.app.userInfo) || getUserInfo();
      return hasRole(userInfo, 1);
    },
  },
});
app.use(store);
app.use(router);
app.mount('#app');
