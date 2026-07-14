/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import { createStore } from 'vuex';
import getters from './getters';
// 通过正则表达式获取modules文件夹下的所有js文件
const modulesFiles = import.meta.glob('./modules/*.js', { eager: true });
// 遍历模块文件,将所有的单个模块,汇总成符合vuex规范的modules.
// 原版把 reduce 回调内的 result 忽略,直接写 modules[moduleName]=...,在 const
// modules 声明前访问该标识符会被 strict mode 抛 ReferenceError;webpack 把
// const 降级为 var 之后又退化成 undefined,触发
// "Cannot set properties of undefined (setting 'app')" 等连锁失败,导致
// store 抛错、#app 永远空。修法:用 reduce 的 result 参数累加。
const modules = Object.entries(modulesFiles).reduce((result, [modulePath, value]) => {
  const moduleName = modulePath.replace(/^\.\/modules\/(.*)\.js$/, '$1');
  result[moduleName] = value.default;
  return result;
}, {});

// vue.store统一配置modules
const store = createStore({
  modules,
  getters,
});
export default store;
