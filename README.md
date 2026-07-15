# 🏥 慧医数字医疗应用系统

> 前后端分离的医疗基础数据管理平台 —— Vue 3 + Spring Boot + MySQL + Redis，内置规范化 RBAC 权限、夜间模式与高德地图集成。

![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-6db33f.svg)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ed.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1.svg)
![Redis](https://img.shields.io/badge/Redis-7-dc382d.svg)
![Platform](https://img.shields.io/badge/平台-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey.svg)

覆盖药品、医生、医药公司、政策、销售地点、城市、必备材料等业务数据，面向后台管理场景提供统一界面、细粒度权限控制与一键容器化部署。

---

## 📖 目录

- [✨ 项目特色](#-项目特色)
- [🚀 快速开始](#-快速开始)
- [🛠 技术栈](#-技术栈)
- [📁 项目结构](#-项目结构)
- [💻 常用命令](#-常用命令)
- [🔌 端口与请求流](#-端口与请求流)
- [⚙️ 配置](#️-配置)
- [📦 业务模块与权限](#-业务模块与权限)
- [🔧 故障排查](#-故障排查)
- [🛡️ 安全底线](#️-安全底线)
- [🤝 贡献指南](#-贡献指南)
- [📄 开源协议](#-开源协议)

## ✨ 项目特色

- 🔐 **规范化 RBAC**：账号-角色-权限多对多模型，动作权限统一 `resource:verb`，菜单 / 动作权限分离查询，后端 `@PreAuthorize` 方法级授权是最终安全边界。
- 🍪 **httpOnly Cookie 会话**：token 不暴露给浏览器 JS，Redis 滑动续期；账号禁用或权限撤销即时生效，无需等待 Cookie / Redis TTL 到期。
- 🌓 **完整夜间模式**：基于 CSS 变量（`--app-*`）+ Element Plus `dark/css-vars`，覆盖表格、表单、弹窗、抽屉等全部组件，一键切换。
- 🗺️ **高德地图集成**：销售地点可视化展示；JS Key 限制域名进前端 bundle，Web 服务 Key 仅由后端 `/api/regeo` 代理调用，密钥分层保护。
- 🐳 **一键 Docker 部署**：Compose 编排 `backend` + `web/nginx`，同源反向代理，固定 tag 版本管理，`docker compose up -d --build` 即用。
- 🛡️ **容器安全加固**：只读根文件系统、非 root 用户（UID 10001）、`no-new-privileges`、`cap_drop ALL`、`/tmp` tmpfs、内存与 CPU 上限。
- 📦 **文件上传安全**：MIME 类型 + 真实图片头校验、UUID 文件名、路径穿越防护，单文件 2 MB 上限。
- 💾 **Redis 双用**：登录会话存储 + dashboard 聚合缓存，业务数据变更自动触发相关缓存失效。
- 🔄 **双仓库同步**：GitHub `main` 与华为云 CodeArts `master` 自动同步，小粒度提交双向推送。
- 🗄️ **数据库迁移版本化**：`medical.sql` 基线 + V2~V5 增量迁移，V5 建立规范化 RBAC 表结构，可追溯、可回滚审查。

## 🚀 快速开始

> 前置：已安装 Docker + Compose v2；MySQL 8 与 Redis 7 可达（默认连实训远程库，也可自备）。

```bash
# 1. 克隆
git clone https://github.com/anmoryli/shixun-swmu.git
cd shixun-swmu

# 2. 首次创建密码 Secret（已存在则跳过）
mkdir -p .work/private/docker && chmod 700 .work/private/docker
printf '%s' '你的MySQL密码' > .work/private/docker/mysql-password.txt
printf '%s' '你的Redis密码' > .work/private/docker/redis-password.txt
chmod 600 .work/private/docker/*-password.txt

# 3. 初始化数据库（仅首次；顺序固定：medical.sql -> V2 -> V3 -> V4 -> V5）
mysql -h <DB_HOST> -u root -p medicine < sql/medical.sql
mysql -h <DB_HOST> -u root -p medicine < sql/migrations/V2__requirements_alignment.sql
mysql -h <DB_HOST> -u root -p medicine < sql/migrations/V3__password_audit_delete_policy.sql
mysql -h <DB_HOST> -u root -p medicine < sql/migrations/V4__sale_map_menu.sql
mysql -h <DB_HOST> -u root -p medicine < sql/migrations/V5__normalized_rbac.sql

# 4. 启动
docker compose up -d --build

# 5. 访问 http://localhost:9092
```

Windows PowerShell 创建 Secret：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\docker\init-secrets.ps1
```

> ⚠️ `sql/medical.sql` 含 `DROP TABLE`，只能在空库执行，不要在已有业务库上重复导入。V2 / V3 / V5 含存储过程，必须用 MySQL 命令行客户端，不能逐句粘贴。

### 🔑 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin_1` | `admin` | 管理员（全部权限） |

> 种子数据密码仅用于实训演示，生产环境务必修改。

## 🛠 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3.5 + Vite 6 + Vuex 4 + Vue Router + Element Plus 2.9 + Less |
| 后端 | Spring Boot 2.7 + Spring Security + MyBatis + Hikari + BCrypt |
| 存储 | MySQL 8（业务库）+ Redis 7（会话 / 缓存） |
| 部署 | Docker Compose（backend + web/nginx） |
| 鉴权 | httpOnly Cookie 会话 + 方法级 `@PreAuthorize` |
| 地图 | 高德地图 JS API + 后端 Web 服务代理 |

## 📁 项目结构

```
medical-managerment-system/  🖥 前端 Vue 3 + Vite
medical-backend/             ⚙️ 后端 Spring Boot + MyBatis
sql/                         🗄 medical.sql 基线 + V2~V5 迁移
compose.yaml                 🐳 Docker Compose 入口
deploy/                      🔐 Secret / systemd / Nginx / 启动脚本
ci/codearts/                 🔄 CodeArts CI/CD
api-tests/                   🧪 Python 回归 + Postman
process-docs/                📚 过程文档与验收证据
```

## 💻 常用命令

```bash
docker compose up -d --build     # 构建并启动（改完代码后必须重建，固定 tag 1.0.0）
docker compose ps                # 查看状态
docker compose logs -f backend   # 跟踪后端日志
docker compose stop              # 停止
docker compose down              # 停止并删容器（保留上传卷，勿加 -v）
```

前端开发：

```bash
cd medical-managerment-system
npm ci
npm run dev       # 开发服务器 http://localhost:9092
npm run build     # 生产构建到 dist/
```

后端构建：

```bash
cd medical-backend
mvn -B -ntp clean package    # 产物 target/medical-backend-1.0.0.jar
mvn -B -ntp clean verify      # 含测试 + 覆盖率门禁
```

## 🔌 端口与请求流

```
浏览器 ── :9092 ──> web(nginx) ──┬─ 静态 SPA
                                 ├─ /api/      ─> backend:8082 ─> MySQL
                                 ├─ /image/    ─> 上传目录
                                 └─ /actuator/ ─> backend:8082 ─> Redis
```

- 前端：`http://localhost:9092`（登录页 `/#/user/login`）
- 后端：`http://localhost:8082`，健康检查 `/actuator/health`
- API 前缀：`/api`，登录态走 httpOnly Cookie

## ⚙️ 配置

通过 `MEDICINE_*` 环境变量覆盖 compose 默认值：

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `MEDICINE_DB_URL` | 实训远程 JDBC | 后端数据库地址 |
| `MEDICINE_REDIS_HOST` | `106.54.210.109` | Redis 地址 |
| `MEDICINE_CORS_ALLOWED_ORIGINS` | `http://localhost:9092,...` | 允许的前端 Origin（不能 `*`） |
| `MEDICINE_COOKIE_SECURE` | `false` | HTTPS 生产设 `true` |
| `MEDICINE_WEB_PORT` / `MEDICINE_BACKEND_PORT` | `9092` / `8082` | 宿主端口 |
| `MEDICINE_BIND_ADDRESS` | `127.0.0.1` | 改 `0.0.0.0` 可局域网访问 |
| `MEDICINE_IMAGE_TAG` | `1.0.0` | 镜像标签 |

密码文件默认在 `.work/private/docker/{mysql,redis}-password.txt`，也可用 `MEDICINE_MYSQL_PASSWORD_FILE` / `MEDICINE_REDIS_PASSWORD_FILE` 指定仓库外绝对路径。完整变量见 `compose.yaml`。

## 📦 业务模块与权限

**业务模块**：首页 dashboard、城市、医药公司、医生、药品、销售地点 / 地图、医保政策、公司政策、必备材料、图片上传。

**RBAC**（V5 规范化）：账号-角色-权限多对多。动作权限统一 `resource:verb`（如 `company:write`、`doctor:reset-password`），菜单权限 `menu:<name>`。前端 `$can()` 控制按钮显隐，后端 `@PreAuthorize` 是最终安全边界。

| 角色 | 权限范围 |
| --- | --- |
| `ADMIN` | 全部菜单 + 全部动作 |
| `DOCTOR` | 业务只读，无写入 / 重置密码 / 上传 |
| `PATIENT` | 默认禁用 |

## 🔧 故障排查

| 现象 | 处理 |
| --- | --- |
| 容器反复退出 | 检查两个 Secret 文件存在且非空，不要同时设明文密码和 `_FILE` |
| 后端 health 非 UP | 查 MySQL / Redis 连通性、迁移是否执行 |
| 登录后立即会话失效 | 确认 Redis 可达、`COOKIE_SECURE` 与域名匹配 |
| 403 / CORS | `CORS_ALLOWED_ORIGINS` 写完整 scheme+host+port，不能 `*` |
| 更新后页面没变 | 固定 tag 1.0.0，必须 `docker compose up -d --build` 重建 |
| 启动报 RBAC 表缺失 | 未执行 V5 迁移，按顺序导入 sql |
| 上传失败 | 文件 < 2 MB、JPG/PNG、上传目录可写 |
| 端口被占用 | `netstat -ano` / `Get-NetTCPConnection`，覆盖 `MEDICINE_*_PORT` |

## 🛡️ 安全底线

- 真实密码只放 Docker Secret 或被忽略的本地私有文件（`0600`），不提交 Git。
- 生产用最小权限账号、内网 / TLS；不用仓库默认公网地址、`root`、`useSSL=false`。
- HTTPS 生产设 `COOKIE_SECURE=true`，CORS 只列实际前端来源。
- 高德 JS Key 会进浏览器 bundle，须在控制台限制域名；Web 服务 Key 只放后端 `AMAP_WEB_KEY`。
- 任何线上数据库操作前先备份；`docker compose down -v` 会删除上传卷，勿当普通重启。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！本项目采用双仓库同步工作流：

1. Fork 仓库并克隆到本地
2. 新建分支：`git switch -c feat/your-feature`
3. 提交代码，遵循约定式提交规范 `<类型>(<范围>): <描述>`
4. 推送后发起 PR 到 `main` 分支

**提交类型**：`feat`（新功能）/ `fix`（修复）/ `docs`（文档）/ `style`（样式）/ `refactor`（重构）/ `test`（测试）/ `chore`（杂项）。

提交示例：

```
feat(auth): 新增医生密码重置接口

Co-Authored-By: Claude <noreply@anthropic.com>
```

双仓库同步规则与推送细节见 [双仓库提交规范.md](双仓库提交规范.md)。

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源，欢迎学习、使用与二次开发。

Copyright © 2026 慧医数字医疗应用系统贡献者。

---

## 📚 更多文档

- [deploy/README.md](deploy/README.md) - 原生部署、systemd、Nginx
- [deploy/docker/README.md](deploy/docker/README.md) - Docker Secret 与 Compose 细节
- [medical-backend/README.md](medical-backend/README.md) - 后端开发
- [medical-managerment-system/README.md](medical-managerment-system/README.md) - 前端开发
- [api-tests/README.md](api-tests/README.md) - 接口回归测试
- [ci/codearts/README.md](ci/codearts/README.md) - CodeArts CI/CD
- [process-docs/开发工程师/07-rbac-technical-design.md](process-docs/开发工程师/07-rbac-technical-design.md) - RBAC 技术设计
- [双仓库提交规范.md](双仓库提交规范.md) - GitHub `main` + CodeArts `master` 双库同步

---

<p align="center">⭐ 如果这个项目对你有帮助，欢迎 Star 支持！</p>
