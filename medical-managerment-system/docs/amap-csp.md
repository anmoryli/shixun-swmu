# 高德地图 CSP 配置说明

## 背景
销售地点地图(SaleMap/SaleManage)使用高德 JSAPI 2.0(@amap/amap-jsapi-loader)。
2.0 矢量瓦片 WebGL 渲染对 CSP 有硬性要求,配置不当会导致底图白屏(marker 本地 DOM 渲染仍显示)。

## 必需放行的 CSP 来源
| directive | 来源 | 用途 |
|---|---|---|
| script-src | 'unsafe-eval' | WebGLRender 模块 eval 动态构造 |
| script-src | 'unsafe-inline' | javascript: callback ___onAPILoaded |
| script-src | https://*.amap.com | SDK 主脚本 webapi.amap.com |
| script-src | https://*.alicdn.com | AWSC/baxia 反爬脚本 |
| img-src | *.amap.com *.autonavi.com *.alicdn.com | 瓦片/sprite/图标 |
| connect-src | 同 img-src | 瓦片数据 fetch、反爬通信 |
| font-src | *.alicdn.com *.amap.com | iconfont 字体、SDF glyph |
| worker-src | 'self' blob: | 高德 blob Worker |

## 诊断方法
下载 SDK 提取所有硬编码域名:
curl -s "https://webapi.amap.com/maps?v=2.0&key=<KEY>" -o /tmp/amap_sdk.js
grep -oE '[a-z0-9._{}-]*\.(amap|autonavi|alicdn)\.[a-z]+' /tmp/amap_sdk.js | sort -u

## 常见报错对照
- EvalError / WebGLRender is not a constructor -> script-src 缺 'unsafe-eval'
- Running the JavaScript URL -> script-src 缺 'unsafe-inline'
- font at.alicdn.com 拦截 -> font-src 缺 *.alicdn.com
- 瓦片 Refused to load -> img-src/connect-src 缺对应域名
