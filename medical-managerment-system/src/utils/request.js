/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router/index';
import { clearAuth } from './authStore';

// 默认使用同源 /api，生产部署时可通过环境变量切换到独立网关。
export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ||
  import.meta.env.VITE_URL ||
  '/api'
).replace(/\/$/, '');

const withCredentials = import.meta.env.VITE_WITH_CREDENTIALS !== 'false';
axios.defaults.withCredentials = withCredentials;

export function resolveApiUrl(path = '') {
  return `${API_BASE_URL}/${String(path).replace(/^\//, '')}`;
}

// 创建 axios 实例
const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 10000,
  withCredentials,
});
// request 拦截器
service.interceptors.request.use(
    (config) => {
        // token 由 httpOnly cookie 承载，浏览器随请求自动携带（withCredentials），无需手动塞 Authorization。
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);
// response 拦截器
service.interceptors.response.use(
    (response) => {
        const res = response.data;
        // code为10006代表token失效，需要重新登录
        if (res.code === 10006) {
            ElMessage({
                type: 'error',
                message: '登录已失效，请重新登录',
            });
            setTimeout(() => {
                clearAuth();
                if (router.currentRoute.value.path !== '/user/login') {
                    router.replace('/user/login');
                }
            }, 500);
        }
        return response;
    },
    (err) => {
        return Promise.reject(err);
    }
);

export default service;
