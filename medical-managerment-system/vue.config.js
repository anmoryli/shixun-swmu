/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

module.exports = {
  publicPath: '/',
  lintOnSave: false, // 关闭ESLint检查
  devServer: {
    host: '0.0.0.0',
    port: '9092', // 代理端口
    proxy: {
      '/api': {
        target: process.env.VUE_APP_PROXY_TARGET || 'http://localhost:8082',
        changeOrigin: true,
        // The browser request is same-origin from the frontend's point of view.
        // Do not forward its public/LAN Origin to the internal backend, otherwise
        // Spring's CORS filter rejects login with 403 when the UI is opened from
        // anything other than the backend's small direct-access allowlist.
        onProxyReq(proxyReq) {
          proxyReq.removeHeader('origin');
        },
      },
    },
  },
};
