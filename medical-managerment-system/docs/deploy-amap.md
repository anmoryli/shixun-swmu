# 地图部署排查

## 白屏排查步骤
1. F12 Console 看 CSP 报错(Refused to load/eval/connect)
2. 对照 docs/amap-csp.md 检查 nginx CSP 是否放行报错域名
3. 改 nginx/default.conf 后必须: docker compose up -d --build web
4. curl -sI http://localhost:9092/ | grep -i content-security-policy 验证
5. 强刷浏览器(Ctrl+F5)清旧 CSP 缓存

## marker 显示但底图白屏
= 瓦片/反爬资源被 CSP 拦截(marker 本地 DOM 渲染不依赖瓦片)。
典型: script-src 缺 'unsafe-eval' 致 WebGLRender 构造失败。

## key 无效
marker 也不显示(AMapLoader.load reject)。检查 .env.production 的 VITE_AMAP_JS_KEY/VITE_AMAP_SECURITY_CODE。
