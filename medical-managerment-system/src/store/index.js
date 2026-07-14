/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import Vue from 'vue';
import Vuex from 'vuex';
import getters from './getters';
Vue.use(Vuex);
// 通过正则表达式获取modules文件夹下的所有js文件
const modulesFiles = require.context('./modules', true, /\.js$/);
// 遍历模块文件,将所有的单个模块,汇总成符合vuex规范的modules.
// 原版把 reduce 回调内的 result 忽略,直接写 modules[moduleName]=...,在 const
// modules 声明前访问该标识符会被 strict mode 抛 ReferenceError;webpack 把
// const 降级为 var 之后又退化成 undefined,触发
// "Cannot set properties of undefined (setting 'app')" 等连锁失败,导致
// store 抛错、#app 永远空。修法:用 reduce 的 result 参数累加。
const modules = modulesFiles.keys().reduce((result, modulePath) => {
  const moduleName = modulePath.replace(/^\.\/(?<name>.*)\.\w+$/, '$<name>');
  const value = modulesFiles(modulePath);
  result[moduleName] = value.default;
  return result;
}, {});

// vue.store统一配置modules
const store = new Vuex.Store({
  modules,
  getters,
});
export default store;