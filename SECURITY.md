# 安全策略

## 报告漏洞

如发现安全漏洞,请勿公开提交 Issue。请通过私有方式联系维护者,我们会在确认后 72 小时内响应并在修复后公开致谢。

## 生产部署安全清单

本项目默认配置面向**实训环境**(便捷优先),生产部署前**必须**完成以下加固。各项均可通过环境变量覆盖,无需改代码。

### 传输层加密
- **数据库**:`DB_URL` 启用 `useSSL=true&verifyServerCertificate=true`,移除 `allowPublicKeyRetrieval`,数据库仅内网/隧道可达。
- **Redis**:内网监听 + 强口令(`REDIS_PASSWORD`),禁止 6379 对公网开放。
- **HTTP**:nginx 启用 TLS(见 `deploy/nginx/medicine.conf` 末尾 HTTPS 占位),设置 `COOKIE_SECURE=true`,开启 HSTS。

### 凭据与密钥管理
- **数据库 / Redis 口令**:通过 Docker Secret 注入(`.work/private/docker/*.txt`,权限 0600,已 gitignore,绝不入库)。
- **高德地图 Key**:
  - JS Key 与安全密钥放在 `.env.local`(已 gitignore),由本地开发/CI 注入;
  - Web 服务 Key 仅后端 `AMAP_WEB_KEY` 持有,前端通过 `/api/regeo` 代理调用,Key 不下发浏览器。
- **历史泄露处理**:git 历史中曾提交过真实高德 Key(commit `453d0c2`),已在 `40a9728` 移出追踪,但因 git 历史不可变,**必须在高德控制台作废该 Key 并重建**。

### 容器与运行时加固
- backend / web 容器均 `cap_drop: ALL` + `no-new-privileges: true` + `read_only: true`(web 用 tmpfs 提供 `/var/cache/nginx`、`/var/run`)。
- web 容器以非 root `nginx` 用户运行,监听非特权端口 8080。
- 登录限流:连续失败 5 次锁定 15 分钟(`app.auth.login-max-attempts` / `app.auth.login-lock-duration`)。
- 登录对不存在用户执行假 BCrypt 比对,防用户名枚举。
- Actuator:`/actuator/health` 的 `show-details=when-authorized`,`/actuator/info` 需认证。
- 业务图片 `/image/**` 需认证(同源 httpOnly cookie 自动携带,UUID 不再可无鉴权下载)。
- nginx 全站下发 `X-Frame-Options`、`X-Content-Type-Options`、`Referrer-Policy` 安全响应头。

### 账号生命周期
- 重置密码生成 8 位随机临时密码(移除硬编码弱口令 `123456`),接口返回供管理员转交。
- 重置密码 / 删除医生账号时,按 `accountId` 失效其所有现有会话(SCAN Redis token,强制重新登录)。

## 已知限制
- `medical_policy.create_time` / `update_time` 为 `varchar`(历史导入数据),系统审计改用新增的 `created_at` / `updated_at`(`datetime`)。
- 本地 http 部署 `COOKIE_SECURE=false`,仅适合内网实训;公网必须 https。
- 默认数据库连接 `useSSL=false`,仅为实训便捷,生产必须改为 `useSSL=true`。
