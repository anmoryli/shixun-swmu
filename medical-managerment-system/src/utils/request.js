/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import axios from 'axios';
import { Message } from 'element-ui';
import router from '../router/index';
import { getToken, clearAuth } from './authStore';

// 默认使用同源 /api，生产部署时可通过环境变量切换到独立网关。
export const API_BASE_URL = (
  process.env.VUE_APP_API_BASE_URL ||
  process.env.VUE_APP_URL ||
  '/api'
).replace(/\/$/, '');

const withCredentials = process.env.VUE_APP_WITH_CREDENTIALS !== 'false';
axios.defaults.withCredentials = withCredentials;

export function resolveApiUrl(path = '') {
  return `${API_BASE_URL}/${String(path).replace(/^\//, '')}`;
}

// 创建 axios 实例
const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: Number(process.env.VUE_APP_API_TIMEOUT) || 10000,
  withCredentials,
});
// request 拦截器
service.interceptors.request.use(
    (config) => {
        const token = getToken();
        if (token) {
            config.headers = config.headers || {};
            config.headers.Authorization = token;
        }
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
            Message({
                type: 'error',
                message: '登录已失效，请重新登录',
            });
            setTimeout(() => {
                clearAuth();
                if (router.currentRoute.path !== '/user/login') {
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
