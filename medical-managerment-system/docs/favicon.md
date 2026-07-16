# Favicon 与 PWA 清单

## 资源
- /favicon.svg: 主标签页图标,药丸医疗主题(teal #14b8a6 + 白)
- /apple-touch-icon.svg: iOS 主屏 180x180 圆角方块
- /site.webmanifest: PWA 清单(名称/主题色/icons)
- /favicon.ico: 旧浏览器兜底

## index.html 声明顺序
1. link rel=icon type=image/svg+xml /favicon.svg (SVG 优先)
2. link rel=icon /favicon.ico sizes=any (兜底)
3. link rel=apple-touch-icon /apple-touch-icon.svg
4. link rel=manifest /site.webmanifest

## 修改后重建
改 public/ 下文件后需 docker compose up -d --build web,vite 才会把 public 复制进 dist。
