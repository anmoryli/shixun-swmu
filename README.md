# 慧医数字医疗应用系统

前后端分离的医疗基础数据管理平台：Vue 3 管理端 + Spring Boot API + MySQL + Redis。覆盖药品、医生、医药公司、政策、销售地点、城市、必备材料等业务数据，内置规范化 RBAC 权限。

## 技术栈

- **前端**：Vue 3 + Vite 6 + Vuex + Vue Router + Element Plus 2 + Less
- **后端**：Spring Boot 2.7 + Spring Security + MyBatis + Hikari + BCrypt
- **存储**：MySQL 8（业务库）+ Redis 7（会话 / 缓存）
- **部署**：Docker Compose（backend + web/nginx）
- **鉴权**：httpOnly Cookie 会话 + 方法级 `@PreAuthorize`

## 快速开始

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

# 3. 初始化数据库（仅首次；顺序固定：medical.sql → V2 → V3 → V4 → V5）
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

> `sql/medical.sql` 含 `DROP TABLE`，只能在空库执行，不要在已有业务库上重复导入。V2/V3/V5 含存储过程，必须用 MySQL 命令行客户端，不能逐句粘贴。

### 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin_1` | `admin` | 管理员（全部权限） |

> 种子数据密码仅用于实训演示，生产环境务必修改。

## 项目结构

```
medical-managerment-system/  前端 Vue 3 + Vite
medical-backend/             后端 Spring Boot + MyBatis
sql/                         medical.sql 基线 + V2~V5 迁移
compose.yaml                 Docker Compose 入口
deploy/                      Secret / systemd / Nginx / 启动脚本
ci/codearts/                 CodeArts CI/CD
api-tests/                   Python 回归 + Postman
process-docs/                过程文档与验收证据
```

## 常用命令

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

## 端口与请求流

```
浏览器 ── :9092 ──> web(nginx) ──┬─ 静态 SPA
                                 ├─ /api/      ─> backend:8082 ─> MySQL
                                 ├─ /image/    ─> 上传目录
                                 └─ /actuator/ ─> backend:8082 ─> Redis
```

- 前端：`http://localhost:9092`（登录页 `/#/user/login`）
- 后端：`http://localhost:8082`，健康检查 `/actuator/health`
- API 前缀：`/api`，登录态走 httpOnly Cookie

## 配置（常用环境变量）

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

## 业务模块与权限

**业务模块**：首页 dashboard、城市、医药公司、医生、药品、销售地点 / 地图、医保政策、公司政策、必备材料、图片上传。

**RBAC**（V5 规范化）：账号–角色–权限多对多。动作权限统一 `resource:verb`（如 `company:write`、`doctor:reset-password`），菜单权限 `menu:<name>`。前端 `$can()` 控制按钮显隐，后端 `@PreAuthorize` 是最终安全边界。

| 角色 | 权限范围 |
| --- | --- |
| `ADMIN` | 全部菜单 + 全部动作 |
| `DOCTOR` | 业务只读，无写入 / 重置密码 / 上传 |
| `PATIENT` | 默认禁用 |

## 故障排查

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

## 安全底线

- 真实密码只放 Docker Secret 或被忽略的本地私有文件（`0600`），不提交 Git。
- 生产用最小权限账号、内网 / TLS；不用仓库默认公网地址、`root`、`useSSL=false`。
- HTTPS 生产设 `COOKIE_SECURE=true`，CORS 只列实际前端来源。
- 高德 JS Key 会进浏览器 bundle，须在控制台限制域名；Web 服务 Key 只放后端 `AMAP_WEB_KEY`。
- 任何线上数据库操作前先备份；`docker compose down -v` 会删除上传卷，勿当普通重启。

## 更多文档

- [deploy/README.md](deploy/README.md) — 原生部署、systemd、Nginx
- [deploy/docker/README.md](deploy/docker/README.md) — Docker Secret 与 Compose 细节
- [medical-backend/README.md](medical-backend/README.md) — 后端开发
- [medical-managerment-system/README.md](medical-managerment-system/README.md) — 前端开发
- [api-tests/README.md](api-tests/README.md) — 接口回归测试
- [ci/codearts/README.md](ci/codearts/README.md) — CodeArts CI/CD
- [process-docs/开发工程师/07-rbac-technical-design.md](process-docs/开发工程师/07-rbac-technical-design.md) — RBAC 技术设计
- [双仓库提交规范.md](双仓库提交规范.md) — GitHub `main` + CodeArts `master` 双库同步
