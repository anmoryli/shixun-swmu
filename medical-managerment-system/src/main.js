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
import './style/ios26-theme.css';

const app = createApp(App);
attachStore(store);
app.use(ElementPlus);
app.config.globalProperties._ = _;
app.mixin({
  computed: {
      // 进行按钮权限控制
      hasRole() {
          const userInfo = getUserInfo();
          if (userInfo) {
              return userInfo.utype === 1;
          }
          return false;
      },
  },
});
app.use(store);
app.use(router);
app.mount('#app');
