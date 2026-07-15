# 慧医数字医疗应用系统

慧医数字医疗应用系统是一个前后端分离的医疗基础数据管理平台，面向药品、医生、医药公司、政策、销售地点、城市和资料等业务数据提供统一的管理界面。项目由 Vue 3 管理端、Spring Boot API、MySQL 业务库和 Redis 会话/缓存组成。

本文是项目的主运行手册，优先给出仓库当前配置可以直接执行的命令。涉及密码、数据库地址、地图 Key 的位置只使用占位符；真实值必须放在本机或服务器的私有配置中，不能提交到 Git。

## 0. 先选择部署方式

| 场景 | 推荐方式 | 前端入口 | 后端健康检查 |
| --- | --- | --- | --- |
| Windows 11/10 本地演示 | Docker Desktop + Compose | http://localhost:9092/ | http://localhost:8082/actuator/health |
| macOS 本地演示 | Docker Desktop + Compose | http://localhost:9092/ | http://localhost:8082/actuator/health |
| Ubuntu/openEuler 服务器 | Docker Engine + Compose v2 | `http://<服务器>:9092/`（也可放到 80/443 反向代理后） | `http://<服务器>:9092/actuator/health` |
| Windows 原生开发 | JAR + Vite 开发服务器 | http://localhost:9092/#/user/login | http://localhost:8082/actuator/health |
| Linux/openEuler 原生生产 | JAR + systemd + Nginx | `http://<域名>/` | `http://<域名>/actuator/health` |
| macOS 原生运行 | JAR + Nginx/Caddy 或前台进程 | 按本机反向代理地址 | http://127.0.0.1:8082/actuator/health |
| 华为云 CodeArts ECS | ci/codearts 中的构建、部署和烟测流水线 | 由部署环境变量决定 | MEDICINE_WEB_PORT 对应的 /actuator/health |

除 Docker 镜像和 Compose 网络外，项目不会自动创建 MySQL 或 Redis。无论选择哪种方式，都必须先准备可连接的 MySQL 8 和 Redis 7，并完成数据库初始化。

## 1. 项目组成与运行边界

~~~text
根目录
├─ medical-managerment-system/  Vue 3 + Vite + Vue Router + Vuex + Element Plus
├─ medical-backend/             Spring Boot 2.7.18 + Spring Security + MyBatis
├─ sql/                         基础 schema/种子数据与 V2、V3、V4、V5 增量迁移
├─ api-tests/                   Python 回归脚本、Postman/Newman 资产
├─ deploy/                      Compose Secret、systemd、Nginx、环境模板和脚本
├─ ci/codearts/                 CodeArts 构建、部署、接口烟测脚本
├─ process-docs/                架构、测试、部署和过程证据
├─ compose.yaml                 当前唯一的根目录 Compose 入口
└─ 双仓库提交规范.md             GitHub 与 CodeArts 的同步规则
~~~

当前默认端口和路径：

- 后端监听 `8082`，API 前缀为 `/api`，健康端点为 `/actuator/health`。
- Compose 前端监听宿主机 `127.0.0.1:9092`，Nginx 在同一来源下代理 `/api/`、`/image/` 和 `/actuator/health`。
- 前端默认构建变量是 `VITE_API_BASE_URL=/api`，浏览器不应在远程部署时直接访问容器名或 `localhost:8082`。
- 登录态通过 `httpOnly Cookie` 保存，前端 Axios 默认 `withCredentials=true`；生产 HTTPS 时应设置 `COOKIE_SECURE=true`。
- 上传文件默认限制为 2 MB，后端只接受 JPG/PNG；上传目录由 `APP_UPLOAD_DIRECTORY` 指定。

## 2. 获取代码和准备运行环境

### 2.1 克隆仓库

GitHub 是日常 `main` 来源，华为云 CodeArts 镜像使用 `master`：

~~~bash
git clone https://github.com/anmoryli/shixun-swmu.git
cd shixun-swmu
git switch main
~~~

CodeArts 镜像地址和双库推送规则见 `双仓库提交规范.md`。不要把真实密码、AK/SK、数据库备份或带密钥的 `.env` 文件加入提交。

### 2.2 软件版本

| 组件 | 版本要求/建议 | 用途 |
| --- | --- | --- |
| JDK | 17（必须） | 编译和运行 Spring Boot |
| Maven | 3.8+，CI 推荐 3.9.x | 后端构建 |
| Node.js | 18+，优先 20 LTS | 前端开发/构建 |
| npm | 随 Node.js | 安装前端锁定依赖 |
| MySQL | 8.x | `medicine` schema |
| Redis | 7.x | 登录会话和 dashboard 缓存 |
| Python | 3.9+ 建议 | `api-tests` 回归脚本（仅标准库） |
| Docker | Engine 现代版本 + Compose v2 | Docker 部署；Dockerfile 使用 BuildKit cache mount |

Windows 使用 Docker Desktop 的 Linux 容器/`desktop-linux` context；macOS 使用 Docker Desktop；Linux/openEuler 使用 Docker Engine 和 Compose v2 插件。先确认：

~~~bash
docker info
docker compose version
java -version
mvn -version
node --version
npm --version
~~~

## 3. 数据库和 Redis 初始化（所有部署方式都需要）

### 3.1 新建空数据库

`sql/medical.sql` 是带种子数据的 MySQL dump，不包含 `CREATE DATABASE` 或 `USE`，并且包含多处 `DROP TABLE`。只能在新建的空库、备份库或明确批准的重置环境执行，不能在已有业务库上重复导入。

~~~sql
CREATE DATABASE IF NOT EXISTS medicine
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
~~~

Linux/macOS、WSL 或 Git Bash 可执行：

~~~bash
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p \
  -e "CREATE DATABASE IF NOT EXISTS medicine CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql/medical.sql
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql/migrations/V2__requirements_alignment.sql
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql/migrations/V3__password_audit_delete_policy.sql
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql/migrations/V4__sale_map_menu.sql
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql/migrations/V5__normalized_rbac.sql
~~~

PowerShell 不建议直接使用 `<` 重定向；可通过 `cmd.exe` 调用 MySQL 客户端：

~~~powershell
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p -e "CREATE DATABASE IF NOT EXISTS medicine CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\medical.sql"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\migrations\V2__requirements_alignment.sql"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\migrations\V3__password_audit_delete_policy.sql"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\migrations\V4__sale_map_menu.sql"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\migrations\V5__normalized_rbac.sql"
~~~

执行顺序必须是 `medical.sql → V2 → V3 → V4 → V5`。V5 依赖 V4 已加入的销售地图菜单，不能提前执行。V2、V3、V5 含 `DELIMITER`/存储过程，应使用 MySQL 命令行客户端，不要用只支持单条语句的普通 SQL 面板逐句粘贴。所有命令都应显式指定 `medicine`；V5 文件内部也带有 `USE medicine` 作为保护。

V5 是规范化 RBAC 的加法迁移：新增 `rbac_role`、`account_role`、`rbac_role_permission` 三张表，为既有 `permission` 补充稳定权限码和 `MENU`/`ACTION` 类型，并从 `account.utype`、`role_permission` 回填兼容数据。迁移不会删除这两个旧入口，便于旧版本应用回滚，但新版本运行时只从规范化关系读取授权。基线完整执行后应为 23 张业务表，可先做最小核验：

~~~sql
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = 'medicine' AND table_type = 'BASE TABLE';

SELECT code, enabled FROM rbac_role ORDER BY code;
SELECT code, permission_type, enabled
FROM permission
ORDER BY permission_type, sort_order, id;

SELECT code, COUNT(*) AS duplicates
FROM permission
GROUP BY code
HAVING code IS NULL OR code = '' OR COUNT(*) > 1;
~~~

预期 `table_count=23`、角色包含启用的 `ADMIN`/`DOCTOR` 和禁用的 `PATIENT`，最后一个重复权限码查询返回空集。已有业务库升级前必须完整备份并在隔离 schema 按同一顺序预演；MySQL DDL 会隐式提交，应用镜像回滚不会撤销 V5。

已有业务库升级时先备份，再逐个审阅迁移脚本；不要把基础 dump 当作升级脚本。容器启动也不会自动导入 SQL。

### 3.2 Redis

准备 Redis 7 实例并确认应用主机/容器可访问 `REDIS_HOST:REDIS_PORT`。生产环境应使用内网监听、强密码和 TLS/VPN；仓库中的公网地址、root 数据库账号、明文 MySQL URL 和空 Redis 密码只为实训默认，不能直接用于生产。

## 4. Docker Compose 部署（Windows、Linux、macOS）

### 4.1 当前拓扑

~~~text
浏览器 ── http://localhost:9092 ──> medicine-web (nginx:1.27)
                                      ├─ 静态 SPA
                                      ├─ /api/ ───────> backend:8082
                                      ├─ /image/ ─────> backend:8082
                                      └─ /actuator/health -> backend:8082

medicine-backend (Temurin JRE 17, UID 10001)
  ├─ 外部 MySQL 8
  ├─ 外部 Redis 7
  └─ medicine_uploads 命名卷
~~~

Compose 只创建 `backend` 和 `web`，不创建 MySQL/Redis。后端镜像使用只读根文件系统、非 root 用户、`/tmp` tmpfs 和资源上限；前端镜像使用 Nginx 同源反向代理。默认宿主绑定均为 `127.0.0.1`：后端 `8082`、前端 `9092`。

### 4.2 Windows Docker Desktop

1. 确认 Docker Desktop 使用 Linux containers，并在仓库根目录执行：

   ~~~powershell
   docker context use desktop-linux
   docker info
   ~~~

2. 如果需要地图功能，在第一次构建前创建被 Git 忽略的 production 环境文件：

   ~~~powershell
   if (-not (Test-Path .\medical-managerment-system\.env.production)) {
       Copy-Item .\medical-managerment-system\.env.example .\medical-managerment-system\.env.production
   }
   notepad .\medical-managerment-system\.env.production
   ~~~

   只填写受域名限制的 `VITE_AMAP_JS_KEY` 和 `VITE_AMAP_SECURITY_CODE`，并删除或留空示例中的 `VITE_AMAP_WEB_KEY`；不要把后端 `AMAP_WEB_KEY` 写进前端文件。该文件会随 Docker build context 进入 Vite 构建并写入浏览器 bundle，因此它不是服务器端 Secret。没有地图 Key 时可跳过此步。

3. 首次创建本机 Docker Secrets：

   ~~~powershell
   powershell -ExecutionPolicy Bypass -File .\deploy\docker\init-secrets.ps1
   ~~~

   脚本会交互输入 MySQL、Redis 密码，生成被 Git 忽略的 `.work\private\docker\mysql-password.txt` 和 `redis-password.txt`。如果远程密码已经轮换，才使用 `-Target MySQL -Force` 或 `-Target Redis -Force` 更新对应文件。

4. 校验 Compose 文件并启动：

   ~~~powershell
   docker compose config --quiet
   docker compose up -d --build
   docker compose ps
   curl.exe -f http://localhost:9092/actuator/health
   curl.exe -f http://localhost:8082/actuator/health
   ~~~

5. 浏览器访问 `http://localhost:9092/`，登录页路由为 `http://localhost:9092/#/user/login`。

远程数据库/Redis 不在 Docker Desktop 内时，先用 `Test-NetConnection <host> -Port 3306` 和 `Test-NetConnection <host> -Port 6379` 检查可达性，再通过 Compose 环境变量覆盖默认地址。

### 4.3 Linux/openEuler 和 macOS Docker Desktop

Docker 命令相同；Linux 需要先安装 Docker Engine 和 Compose v2，macOS 需要启动 Docker Desktop。需要地图功能时，在第一次构建前创建被 Git 忽略的 production 环境文件：

~~~bash
if [ ! -f medical-managerment-system/.env.production ]; then
    cp medical-managerment-system/.env.example medical-managerment-system/.env.production
fi
${EDITOR:-vi} medical-managerment-system/.env.production
~~~

只填写受域名限制的 `VITE_AMAP_JS_KEY` 和 `VITE_AMAP_SECURITY_CODE`，并删除或留空示例中的 `VITE_AMAP_WEB_KEY`；不要把后端 `AMAP_WEB_KEY` 写进前端文件。该文件会在 Vite 构建阶段进入浏览器 bundle，不能当作服务器端 Secret。没有地图 Key 时可跳过此步。

首次创建 POSIX Secret（在 bash/zsh 中执行）：

~~~bash
mkdir -p .work/private/docker
chmod 700 .work/private/docker
umask 077
printf 'MySQL password: '
IFS= read -r -s MYSQL_PASSWORD
printf '\n'
printf '%s' "$MYSQL_PASSWORD" > .work/private/docker/mysql-password.txt
unset MYSQL_PASSWORD
printf 'Redis password: '
IFS= read -r -s REDIS_PASSWORD
printf '\n'
printf '%s' "$REDIS_PASSWORD" > .work/private/docker/redis-password.txt
unset REDIS_PASSWORD
chmod 600 .work/private/docker/*-password.txt
~~~

从仓库根目录启动：

~~~bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
curl --fail http://127.0.0.1:9092/actuator/health
curl --fail http://127.0.0.1:8082/actuator/health
~~~

服务器对外提供服务时，不要直接把 `8082` 暴露到公网；优先让外部 Nginx/负载均衡终止 HTTPS 并只开放 80/443。若确需局域网演示，可在启动前覆盖：

~~~bash
export MEDICINE_BIND_ADDRESS=0.0.0.0
export MEDICINE_WEB_PORT=9092
docker compose up -d --build
~~~

### 4.4 Compose 配置覆盖

Compose 从当前 shell 环境读取 `MEDICINE_*` 变量，也可使用 `docker compose --env-file <file>`。常用变量：

| 变量 | 默认值 | 作用 |
| --- | --- | --- |
| `MEDICINE_DB_URL` | 实训 MySQL URL | 后端 JDBC 地址；生产改内网/TLS |
| `MEDICINE_DB_USERNAME` | `root` | 数据库账号，生产使用最小权限账号 |
| `MEDICINE_REDIS_HOST` / `MEDICINE_REDIS_PORT` | `106.54.210.109` / `6379` | Redis 地址 |
| `MEDICINE_REDIS_DATABASE` | `0` | Redis database |
| `MEDICINE_CORS_ALLOWED_ORIGINS` | `http://localhost:9092,http://127.0.0.1:9092` | 精确允许的前端 Origin，不能用 `*` 配合 Cookie |
| `MEDICINE_COOKIE_SECURE` | `false` | HTTPS 生产设为 `true` |
| `MEDICINE_AMAP_WEB_KEY` | 空 | 仅后端持有的高德 Web 服务 Key |
| `MEDICINE_API_BASE_URL` | `/api` | 前端构建参数，推荐保持同源 `/api` |
| `MEDICINE_BIND_ADDRESS` / `MEDICINE_WEB_PORT` | `127.0.0.1` / `9092` | Web 宿主绑定和端口 |
| `MEDICINE_BACKEND_BIND` / `MEDICINE_BACKEND_PORT` | `127.0.0.1` / `8082` | 后端宿主绑定和端口 |
| `MEDICINE_IMAGE_TAG` | `1.0.0` | 镜像标签 |
| `MEDICINE_UPLOAD_VOLUME` | `medicine_uploads` | 上传持久卷名称 |

密码文件默认位于 `.work/private/docker/`；也可以通过 `MEDICINE_MYSQL_PASSWORD_FILE` 和 `MEDICINE_REDIS_PASSWORD_FILE` 指定仓库外的绝对路径。项目名、网络名和资源上限还可用 `MEDICINE_PROJECT_NAME`、`MEDICINE_NETWORK_NAME`、`MEDICINE_BACKEND_MEMORY`、`MEDICINE_BACKEND_CPUS`、`MEDICINE_WEB_MEMORY` 和 `MEDICINE_WEB_CPUS` 覆盖。

PowerShell 示例：

~~~powershell
$env:MEDICINE_DB_URL = 'jdbc:mysql://10.0.0.10:3306/medicine?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=true&verifyServerCertificate=true'
$env:MEDICINE_DB_USERNAME = 'medicine_app'
$env:MEDICINE_REDIS_HOST = '10.0.0.11'
$env:MEDICINE_CORS_ALLOWED_ORIGINS = 'https://medicine.example.com'
$env:MEDICINE_COOKIE_SECURE = 'true'
docker compose up -d --build
~~~

bash/zsh 示例：

~~~bash
export MEDICINE_DB_URL='jdbc:mysql://10.0.0.10:3306/medicine?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=true&verifyServerCertificate=true'
export MEDICINE_DB_USERNAME=medicine_app
export MEDICINE_REDIS_HOST=10.0.0.11
export MEDICINE_CORS_ALLOWED_ORIGINS='https://medicine.example.com'
export MEDICINE_COOKIE_SECURE=true
docker compose up -d --build
~~~

如果部署独立域名的前端，仍建议构建 `MEDICINE_API_BASE_URL=/api`，让 Nginx 处理同源代理；不要把 `http://localhost:8082/api` 编进供其他电脑访问的前端包。

### 4.5 启停、日志、升级和回滚

~~~bash
docker compose ps
docker compose logs -f --tail 100 backend web
docker compose stop
docker compose start
docker compose down
docker compose up -d --build
~~~

`docker compose down` 会删除容器和网络但保留 `medicine_uploads`。不要执行 `docker compose down -v`，它会删除上传数据。升级前可为当前镜像打备份标签：

~~~bash
docker tag medicine-backend:1.0.0 medicine-backend:backup-YYYYMMDD
docker tag medicine-web:1.0.0 medicine-web:backup-YYYYMMDD
~~~

回滚时设置 `MEDICINE_IMAGE_TAG=backup-YYYYMMDD`，再执行：

~~~bash
docker compose up -d --no-build --force-recreate
~~~

数据库迁移需要单独备份和审阅；回滚应用镜像不会自动回滚数据库结构。

## 5. 非 Docker 部署

非 Docker 模式把后端 JAR、前端 `dist/`、MySQL、Redis 和反向代理分别管理。建议生产使用 Linux/openEuler 的 systemd + Nginx；Windows/macOS 更适合开发、演示或由各自的服务管理器托管。

### 5.1 通用构建

后端：

~~~bash
cd medical-backend
mvn -B -ntp clean package
~~~

产物为 `target/medical-backend-1.0.0.jar`。持续集成的严格门禁使用 `mvn -B -ntp clean verify`，其中 JaCoCo 配置要求行覆盖率达到 100%。

前端：

~~~bash
cd medical-managerment-system
npm ci
npm run build
~~~

产物为 `dist/`。前端地图需要 `VITE_AMAP_JS_KEY` 和 `VITE_AMAP_SECURITY_CODE`。生产构建前复制被忽略的本地环境文件并填写受域名限制的 JS Key；高德 Web 服务 Key 不要放入 `VITE_*`，只通过后端 `AMAP_WEB_KEY` 注入。没有地图 Key 时主系统仍可运行，但地图功能不可用：

~~~bash
if [ ! -f .env.production ]; then
    cp .env.example .env.production
fi
${EDITOR:-vi} .env.production
~~~

开发模式使用同样的内容复制为 `.env.development`。删除或留空示例中的 `VITE_AMAP_WEB_KEY`；`.env.production` 会进入 Vite 构建产物，JS Key 只能视为公开配置，必须在高德控制台限制域名；不要把数据库密码或后端 Web 服务 Key 放进去。

### 5.2 Windows 原生运行（PowerShell）

1. 安装 JDK 17、Maven、Node.js 18+ 和 MySQL/Redis 客户端；按第 3 节准备远程或本机数据库。
2. 在仓库根目录创建被忽略的 `.work\private\medicine-backend.env.ps1`，内容使用 PowerShell 赋值，而不是 `KEY=value`：

   ~~~powershell
   $env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/medicine?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false'
   $env:DB_USERNAME = 'medicine_app'
   $env:DB_PASSWORD = '<MySQL 密码>'
   $env:REDIS_HOST = '127.0.0.1'
   $env:REDIS_PORT = '6379'
   $env:REDIS_PASSWORD = '<Redis 密码>'
   $env:APP_UPLOAD_DIRECTORY = 'C:\path\to\shixun-swmu\uploads'
   $env:CORS_ALLOWED_ORIGINS = 'http://localhost:9092'
   ~~~

3. 构建后端并用仓库脚本启动：

   ~~~powershell
   cd .\medical-backend
   mvn -B -ntp clean package
   cd ..
   powershell -ExecutionPolicy Bypass -File deploy\scripts\start-local.ps1 -Port 8082
   ~~~

   `start-local.ps1` 默认读取上述私有文件，强制要求 `DB_PASSWORD` 和 `REDIS_PASSWORD`，并支持 `-EnvFile`、`-Port`、`-Address`。如果不指定 `APP_UPLOAD_DIRECTORY`，上传目录会相对当前调用目录生成，生产/演示建议使用绝对路径。

4. 另开 PowerShell 窗口启动前端开发服务器：

   ~~~powershell
   cd .\medical-managerment-system
   npm ci
   if (-not (Test-Path .env.development)) {
       Copy-Item .env.example .env.development
   }
   notepad .env.development
   $env:VITE_PROXY_TARGET = 'http://localhost:8082'
   npm run dev -- --host 0.0.0.0 --port 9092
   ~~~

   浏览器访问 `http://localhost:9092/#/user/login`。Vite 只在开发服务器中代理 `/api`；生产发布请使用 Nginx/IIS 等带 SPA fallback 和 `/api` 反向代理的服务器。

5. 只检查后端健康和错误登录契约可执行：

   ~~~powershell
   powershell -ExecutionPolicy Bypass -File deploy\scripts\verify-deployment.ps1 -BaseUrl http://localhost:8082
   ~~~

### 5.3 Linux/openEuler 原生生产（systemd + Nginx）

以下目录和服务模板与 `deploy/` 一致。命令需要 root 权限，路径可按实际磁盘调整：

~~~bash
sudo useradd --system --home-dir /opt/medicine --shell /usr/sbin/nologin medicine || true
sudo install -d -o medicine -g medicine /opt/medicine/app /opt/medicine/web /opt/medicine/uploads /opt/medicine/logs
sudo install -d -m 700 /etc/medicine

sudo install -o root -g root -m 644 medical-backend/target/medical-backend-1.0.0.jar \
  /opt/medicine/app/medical-backend-1.0.0.jar
sudo cp -a medical-managerment-system/dist/. /opt/medicine/web/

sudo install -m 600 deploy/env/medicine-backend.env.example \
  /etc/medicine/medicine-backend.env
sudoedit /etc/medicine/medicine-backend.env
~~~

至少填写数据库、Redis、`APP_UPLOAD_DIRECTORY=/opt/medicine/uploads`、`SERVER_ADDRESS=127.0.0.1`、实际 `CORS_ALLOWED_ORIGINS` 和 `AMAP_WEB_KEY`。HTTPS 站点还应设置 `COOKIE_SECURE=true`、更短的 `TOKEN_TTL`，并把数据库 URL 改为 TLS/内网地址。

安装仓库模板并启动：

~~~bash
sudo install -m 644 deploy/systemd/medicine-backend.service \
  /etc/systemd/system/medicine-backend.service
sudo install -m 644 deploy/nginx/medicine.conf \
  /etc/nginx/conf.d/medicine.conf
sudo systemctl daemon-reload
sudo systemctl enable --now medicine-backend
sudo nginx -t
sudo systemctl reload nginx
~~~

`deploy/nginx/medicine.conf` 默认监听 80、静态发布 `/opt/medicine/web`，将 `/api/` 和 `/actuator/health` 代理到 `127.0.0.1:8082`，将 `/image/` 映射到上传目录，并提供 Vue history fallback。生产防火墙只开放 80/443，不直接开放 8082。

运维命令：

~~~bash
sudo systemctl status medicine-backend
sudo journalctl -u medicine-backend --since "10 min ago"
sudo tail -F /opt/medicine/logs/backend.out.log /opt/medicine/logs/backend.err.log
curl --fail http://127.0.0.1:8082/actuator/health
curl --fail http://<域名>/actuator/health
~~~

升级时先备份 JAR、`dist/` 和上传目录，替换产物后执行 `sudo systemctl restart medicine-backend`，再用 `nginx -t && systemctl reload nginx` 平滑更新前端。失败时恢复上一个 JAR/dist 并重新检查健康端点。

### 5.4 macOS 原生运行

macOS 没有 systemd unit；开发或小型演示可使用前台 JAR，长期运行可使用 launchd、Homebrew service 或其他进程管理器。可选的 Homebrew 依赖：

~~~bash
brew install openjdk@17 maven node@20 nginx
export JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
~~~

后端环境变量和启动命令与 Linux 相同：

~~~bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/medicine?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false'
export DB_USERNAME=medicine_app
export DB_PASSWORD='<MySQL 密码>'
export REDIS_HOST=127.0.0.1 REDIS_PORT=6379 REDIS_PASSWORD='<Redis 密码>'
export APP_UPLOAD_DIRECTORY="$PWD/uploads" SERVER_PORT=8082
mkdir -p "$APP_UPLOAD_DIRECTORY"
cd medical-backend
mvn -B -ntp clean package
exec java -XX:MaxRAMPercentage=70 -jar target/medical-backend-1.0.0.jar
~~~

前端执行 `npm ci && npm run build`，使用 Nginx/Caddy 托管 `dist/`，并把 `/api`、`/image` 和 `/actuator/health` 代理到 `127.0.0.1:8082`。复制 `deploy/nginx/medicine.conf` 后将 Linux 路径改成 macOS 的绝对路径；`npm run preview` 只用于预览，不应作为高可用生产守护进程。

### 5.5 Windows 原生生产前端

仓库没有 IIS URL Rewrite 或 Windows 服务注册模板。可以使用 Windows Nginx、IIS+URL Rewrite/ARR 或其他支持 SPA fallback 的静态服务器：

1. `npm run build` 生成 `dist/`。
2. 将站点根目录指向 `dist/`，未知路径回退到 `index.html`。
3. 将 `/api/`、`/image/` 和 `/actuator/health` 反向代理到后端 `http://127.0.0.1:8082`。
4. 后端 `CORS_ALLOWED_ORIGINS` 填写实际前端 Origin；HTTPS 时设置 `COOKIE_SECURE=true`。

如果暂时没有反向代理，也可以在构建前设置完整 API 地址：

~~~powershell
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8082/api'
$env:VITE_WITH_CREDENTIALS = 'true'
cd .\medical-managerment-system
npm ci
npm run build
~~~

这种分域/分端口方式必须同步配置精确的 CORS Origin，不能使用 `*`；正式环境仍推荐同源 HTTPS 反向代理。

## 6. 配置和安全底线

- 真实 MySQL/Redis 密码只放 Docker Secret、`/etc/medicine/medicine-backend.env` 或被忽略的本地私有文件，并设置 `0600`。
- 生产数据库使用最小权限账号、内网/VPN/TLS；不要使用仓库默认公网地址、`root` 或 `useSSL=false`。
- Redis 不应对公网开放；会话和 dashboard 缓存都依赖它。
- HTTPS 生产设置 `COOKIE_SECURE=true`，`CORS_ALLOWED_ORIGINS` 只列实际前端来源，保留 `withCredentials`。
- 高德 JS Key 会进入浏览器 bundle，应在高德控制台限制域名；高德 Web 服务 Key 只放后端 `AMAP_WEB_KEY`，不要写成 `VITE_*`。
- 上传目录必须可写但不应允许执行脚本；Nginx 限制请求体为 2 MB。
- 不要把 `docker compose down -v` 用作普通重启；它会删除上传卷。
- 基础 SQL 有 `DROP TABLE`，任何线上数据库操作前先备份并走迁移审查。

## 7. 产品范围、角色和业务模块

### 7.1 角色与边界

- 管理员（规范化角色 `ADMIN`，兼容字段 `account.utype=ROLE_1`）维护基础数据、医生账号、政策、药品、销售地点和必备材料，可执行写入、删除和医生密码重置。
- 医生/普通用户（规范化角色 `DOCTOR`，兼容字段 `account.utype=ROLE_2`）登录后读取授权菜单和业务数据；写入权限由后端方法级授权控制，不能只依赖前端按钮隐藏。
- 未登录用户只能访问登录、会话恢复、健康检查和公开图片 GET 等明确放行的接口。

项目是后台管理系统，不包含患者端挂号、诊疗、电子病历、处方、支付或医院核心系统集成。

### 7.2 主要业务

首页 dashboard 聚合统计和资讯摘要并使用 Redis 缓存；业务变更会触发相关缓存失效。业务模块包括城市、医药公司、医生、药品、销售地点/地图、医保政策、公司政策、必备材料和图片上传。高德 JS Key 仅用于浏览器地图；高德 Web 服务 Key 由后端的 `GET /api/regeo` 代理使用。

### 7.3 规范化 RBAC 和权限码

V5 使用“账号—角色—权限”的多对多模型。`rbac_role` 保存稳定角色编码，`account_role` 允许一个账号关联多个角色，`permission` 同时保存动态菜单（`MENU`）和后端动作（`ACTION`），`rbac_role_permission` 保存授权关系。`account.utype` 和旧 `role_permission` 只作为迁移、兼容和旧版本回滚数据保留，不再是新运行时授权查询入口。

动作权限统一使用 `resource:verb`，控制器和前端使用同一字符串：

| 资源 | 只读权限 | 写入/特殊权限 |
| --- | --- | --- |
| 首页 | `dashboard:read` | - |
| 医药公司 | `company:read` | `company:write` |
| 销售地点/地图 | `sale:read`、`sale-map:read` | `sale:write` |
| 城市 | `city:read` | `city:write` |
| 药品 | `drug:read` | `drug:write` |
| 医保政策 | `medical-policy:read` | `medical-policy:write` |
| 公司政策 | `company-policy:read` | `company-policy:write` |
| 医生 | `doctor:read` | `doctor:write`、`doctor:reset-password` |
| 必备材料 | `material:read` | `material:write` |
| 文件 | - | `file:upload` |

菜单权限使用 `menu:<name>`，与动作权限按 `permission_type` 严格分开查询。管理员获得全部启用菜单和动作权限；医生保留业务只读权限，没有写入、密码重置或上传权限；患者角色默认禁用。前端 `$can()`、按钮隐藏和路由白名单只负责交互体验，后端 `@PreAuthorize("hasAuthority('resource:verb')")` 才是最终安全边界。

## 8. 架构和请求流

~~~text
浏览器
  │  同源 HTTPS/HTTP，Axios withCredentials
  ▼
Nginx / Vite proxy
  ├─ 静态 Vue SPA（Hash 路由）
  ├─ /api/* ─────── Spring Security → Controller → Service → MyBatis → MySQL
  ├─ /image/* ───── 上传文件目录/命名卷
  └─ /actuator/* ── 健康与信息端点
                         ├─ Redis：登录会话、dashboard 缓存
                         └─ 高德 Web API：/api/regeo 后端代理
~~~

后端分层位于 `com.medicine` 下的 `auth`、`business`、`dashboard`、`amap`、`config`、`security`、`common` 和 `web`；前端通过 `src/api`、`src/store`、`src/router` 和 `src/views` 组织页面、接口和状态。

安全实现包括 BCrypt 密码校验、Spring Security 方法级动作权限授权、httpOnly Cookie 会话、Redis 会话摘要/滑动续期、上传 MIME 与真实图片校验、UUID 文件名和路径穿越防护。每个已认证请求都以 Redis 会话中的 `userId` 重新查询启用账号、启用角色和启用动作权限，再构造 Spring Security authorities；账号禁用或权限撤销不必等待 Cookie/Redis TTL 到期。浏览器默认不读取 token；脚本客户端仍可按后端兼容规则使用 Authorization 头。

## 9. 目录和模块速查

| 路径 | 内容 |
| --- | --- |
| `medical-managerment-system/src/api/` | 登录、dashboard 和各管理模块 API |
| `medical-managerment-system/src/components/` | 通用组件、分页和主题 |
| `medical-managerment-system/src/layout/` | 登录后布局、侧栏和导航 |
| `medical-managerment-system/src/router/` | 常量路由和后端菜单注册 |
| `medical-managerment-system/src/store/` | Vuex 状态、会话恢复和业务模块 |
| `medical-managerment-system/src/views/` | Login、Home、Company、Doctor、Drug、Material、Policy、Sale 等页面 |
| `medical-backend/src/main/java/` | Controller、Service、Mapper、Security、缓存和异常处理 |
| `medical-backend/src/main/resources/application.yml` | Spring 环境变量默认值和运行参数 |
| `sql/medical.sql` | 基础表结构和种子数据（破坏性初始化） |
| `sql/migrations/` | V2/V3/V4/V5 增量迁移；V5 建立规范化 RBAC |
| `deploy/` | 本地 Secret、systemd、Nginx、环境模板、启动/验证脚本 |
| `api-tests/` | Python black-box runner、单元测试和 Postman 集合 |
| `ci/codearts/` | CodeArts 构建包、部署回滚和烟测 |
| `process-docs/` | 需求、架构、测试、部署和验收证据 |

## 10. 配置参考

### 10.1 后端环境变量

后端使用 Spring relaxed binding，Compose 的 `APP_UPLOAD_DIRECTORY` 会映射到 `app.upload.directory`。敏感变量不要写进 `application.yml`。

| 变量 | 默认/示例 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8082` | Tomcat 端口 |
| `SERVER_ADDRESS` | Compose 为 `0.0.0.0` | 原生 Nginx 模式建议 `127.0.0.1` |
| `DB_URL` | 实训公网 JDBC URL | 生产使用内网/TLS URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / 空 | 改为最小权限账号和私密密码 |
| `DB_CONNECTION_TIMEOUT_MS` / `DB_POOL_SIZE` | `10000` / `10` | Hikari 参数 |
| `REDIS_HOST` / `REDIS_PORT` | 实训地址 / `6379` | Redis 地址 |
| `REDIS_PASSWORD` / `REDIS_DATABASE` | 空 / `0` | 会话与缓存凭据 |
| `TOKEN_TTL` | `7d` | 会话 TTL |
| `COOKIE_NAME` / `COOKIE_DOMAIN` / `COOKIE_PATH` | `medicine_token` / 空 / `/` | Cookie 属性 |
| `COOKIE_SECURE` / `COOKIE_SAME_SITE` | `false` / `lax` | HTTPS 生产设 Secure |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:9092,http://127.0.0.1:9092` | 精确 Origin，不能用 `*` 配 Cookie |
| `AMAP_WEB_KEY` | 空 | 仅后端使用的高德 Web 服务 Key |
| `APP_UPLOAD_DIRECTORY` | `uploads` | 生产使用绝对路径 |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | `2MB` | 单文件上限 |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | `3MB` | 请求上限 |
| `APP_LOG_LEVEL` | `INFO` | 应用日志级别 |
| `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | Compose 为 `never` | 生产不要暴露依赖细节 |

Docker Secret 通过 `DB_PASSWORD_FILE` 和 `REDIS_PASSWORD_FILE` 读取；不要同时设置对应的明文环境变量，否则入口脚本会拒绝启动。

同一主机不同端口通常仍属于同一站点，默认 `COOKIE_SAME_SITE=lax` 可以工作。如果前端和 API 位于不同站点，必须使用 HTTPS、`COOKIE_SECURE=true`、`COOKIE_SAME_SITE=none`，并准确配置 CORS；浏览器还可能依据第三方 Cookie 策略拦截会话。当前根 `compose.yaml` 显式映射了 `MEDICINE_COOKIE_SECURE`，没有映射 `MEDICINE_COOKIE_SAME_SITE`；跨站 Docker 部署前应先在 Compose 环境中补充该映射，原生部署则直接在后端环境文件设置。

### 10.2 前端构建变量

这些变量在 Vite 构建时写入 bundle；不要放数据库密码或后端 Web 服务 Key。

| 变量 | 默认/示例 | 说明 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api` | 推荐同源 API 前缀 |
| `VITE_PROXY_TARGET` | `http://localhost:8082` | Vite 开发代理目标 |
| `VITE_API_TIMEOUT` | `10000` | Axios 超时毫秒数 |
| `VITE_WITH_CREDENTIALS` | `true` | Cookie 会话必须保持 true |
| `VITE_AMAP_JS_KEY` | 占位符 | 可公开但应限制域名 |
| `VITE_AMAP_SECURITY_CODE` | 占位符 | 高德 JS 安全密钥 |
| `VITE_URL` | 兼容旧配置 | API base 的回退变量 |

仓库只跟踪 `medical-managerment-system/.env.example`；`.env.development`、`.env.production` 和 `.env.local` 属于本机文件。干净 clone 没有地图 Key 时可以先构建，地图页面需要再补齐 JS Key 和安全密钥。

## 11. API、认证和客户端约定

### 11.1 通用响应

业务接口通常返回：

~~~json
{"code": 20000, "message": "success", "data": {}}
~~~

常见 code：`20000` 成功、`10000` 参数错误、`10001` 重复数据、`10002` 登录失败、`10003` 无权限、`10004` 不存在、`10006` 会话失效、`50000` 服务端错误。实际错误以响应体为准，不能只看 HTTP 200。

### 11.2 认证流程

1. `POST /api/login` 支持表单和 JSON；成功后写入 httpOnly Cookie，响应数据为用户信息，不把 token 暴露给浏览器 JavaScript。
2. 刷新页面时 `GET /api/session` 从 Cookie/Redis 恢复用户；失败时前端清理本地状态并回登录页。
3. 每个受保护请求由过滤器从 Cookie/Authorization 解析 Token，经 Redis 恢复 `userId`，再从 MySQL 查询当前启用角色和动作权限；没有有效角色或目标权限时默认拒绝。
4. `GET /api/permissions` 按认证主体的账号 ID 返回 `permissions` 菜单树、`permissionCodes` 动作权限码和 `roles`，不信任也不发送客户端选择的 `roleName`。
5. 前端只在内存/Vuex 保存用户、权限码和已授权路由；退出、会话失效或账号切换时清空能力并卸载旧动态路由。
6. `POST /api/logout` 删除当前会话并清理 Cookie。
7. 后端过滤器仍兼容 `Authorization: Bearer <token>` 或裸 token，方便 Postman/脚本；浏览器前端优先使用 Cookie。

### 11.3 主要资源入口

| 资源 | 路径 |
| --- | --- |
| 登录、会话、权限、退出 | `/api/login`、`/api/session`、`/api/permissions`、`/api/logout` |
| 首页和地图 | `/api/dashboard`、`/api/regeo` |
| 文件 | `POST /api/base/upload`、公开 `GET /image/**` |
| 城市 | `/api/citys` |
| 医药公司/政策 | `/api/companys`、`/api/company_policys` |
| 医生/药品 | `/api/doctors`、`/api/drugs` |
| 材料/医保政策 | `/api/materials`、`/api/medical_policys` |
| 销售地点 | `/api/sales` |

完整方法、参数和权限以 Controller、前端 `src/api` 和 Postman collection 为准。

## 12. 构建、测试和质量门禁

### 12.1 本地命令

后端快速单测：

~~~bash
cd medical-backend
mvn -B -ntp test
~~~

后端准出构建（包含 JaCoCo 行覆盖率门禁）：

~~~bash
mvn -B -ntp clean verify
~~~

前端测试、覆盖率和构建：

~~~bash
cd medical-managerment-system
npm ci
npm test
npm run test:coverage
npm run build
~~~

API runner 自身的标准库单测：

~~~bash
python -m unittest discover -s api-tests -p 'test_*.py'
~~~

部署后的 live runner 需要 `BASE_URL`、`ADMIN_USERNAME`、`ADMIN_PASSWORD`。当前认证已切换为 httpOnly Cookie，而旧 runner/部分 Postman 资产仍按响应 `data.token` 取 token；在未更新为 CookieJar 之前，不要把历史 57/57 报告当作当前 live runner 保证。这个兼容性修复应作为独立代码任务处理。

### 12.2 仓库脚本和报告

| 入口 | 作用 |
| --- | --- |
| `deploy/scripts/ci-build.ps1 all` | Windows 本地 CI 等价构建；`SKIP_TESTS=false` 才执行完整测试 |
| `bash deploy/scripts/ci-build.sh all false` | Linux/macOS 构建并运行测试 |
| `api-tests/run_api_tests.py` | 部署后的黑盒回归，默认生成 JSON/JUnit/Markdown 证据 |
| `api-tests/postman/` | Postman/Newman 和 CodeArts TestPlan 资产 |
| `process-docs/测试工程师/02-test-report-100-line-coverage-20260715.md` | 最新代码级覆盖率过程报告 |
| `process-docs/开发工程师/06-acceptance-summary.md` | 验收摘要 |

覆盖率报告是特定提交和环境的证据；每次代码或依赖变化后应重新运行，不要把历史数字当成当前运行结果。

## 13. CodeArts CI/CD

`ci/codearts/README.md` 将流水线分为“构建和检查”和“部署和测试”：

~~~text
Repo(master)
  ├─ Maven clean verify + npm ci/build
  ├─ Java/JavaScript/Python 代码检查
  └─ 生成 ci-output/medicine-cicd.tar.gz
       ▼
部署 ECS（版本目录 + current 链接）
  ├─ Docker Compose v2
  ├─ backend/web 健康门禁
  └─ 不健康时尝试恢复上一版本
       ▼
接口烟测（TestPlan 或 ci/codearts/api-test.sh）
~~~

ECS 需要 Docker Engine、Compose v2、`/etc/medicine/medicine-ci.env` 和权限为 600 的 MySQL/Redis Secret 文件。核心命令：

~~~bash
bash ci/codearts/build.sh
bash ci/codearts/deploy.sh <artifact> /opt/medicine
BASE_URL=http://<ECS>:9092 ADMIN_USERNAME='<账号>' ADMIN_PASSWORD='<密码>' \
  bash ci/codearts/api-test.sh
~~~

CodeArts 使用仓库 `master`，本地开发分支是 `main`；不要把流水线密码、AK/SK 或带密钥环境文件提交到 Git。

## 14. 日常运维、备份和回滚

### 14.1 健康和日志

Docker：

~~~bash
docker compose ps
docker compose logs --tail 100 backend
docker compose logs --tail 100 web
curl --fail http://127.0.0.1:8082/actuator/health
curl --fail http://127.0.0.1:9092/actuator/health
~~~

systemd/Nginx：

~~~bash
sudo systemctl status medicine-backend
sudo journalctl -u medicine-backend --since "10 min ago"
sudo tail -F /opt/medicine/logs/backend.out.log /opt/medicine/logs/backend.err.log
sudo nginx -t
curl --fail http://127.0.0.1:8082/actuator/health
~~~

Web 容器的健康检查只确认 `index.html` 可读；后端数据库、Redis 是否健康必须单独查看后端健康端点。

### 14.2 备份

上线前至少备份：

- MySQL：使用与服务器版本匹配的 `mysqldump --single-transaction`，并安全保存备份。
- Redis：按 Redis 运维策略保存 RDB/AOF；不要把会话备份当作数据库备份。
- 上传目录：Docker 的 `medicine_uploads` 卷或原生上传目录。
- 当前 JAR、`dist/`、Compose 环境和 Secret 文件（Secret 只能保存到仓库外安全位置）。

备份恢复应在隔离环境演练；不要用 `sql/medical.sql` 验证生产数据库。

### 14.3 应用回滚

应用回滚只替换 JAR/镜像和前端静态文件，不会自动回滚 SQL。先停止或切换流量，再恢复上一个版本，最后重复健康、登录和关键读接口检查。数据库迁移若不可逆，必须准备单独的回滚脚本和备份恢复方案。

## 15. 常见故障排查

| 现象 | 先看什么 | 处理方向 |
| --- | --- | --- |
| 9092/8082 被占用 | `netstat -ano`、`Get-NetTCPConnection` | 覆盖 Compose 端口或停止占用进程 |
| 容器反复退出 | `docker compose logs backend` | 检查两个 Secret 是否存在、非空；不要同时设明文密码和 `_FILE` |
| 后端 health 非 UP | 健康端点、后端日志 | 检查 MySQL/Redis DNS、端口、账号、TLS 和迁移状态 |
| 前端页面 200 但 API 失败 | 浏览器 Network、Nginx 日志 | 保持 `VITE_API_BASE_URL=/api`，确认 `/api/` 代理 |
| 登录后立即 10006 | Cookie、`COOKIE_SECURE`、域名和时间 | HTTPS 才设 Secure；确认 credentials、Redis 可用 |
| 403/CORS | Origin、`CORS_ALLOWED_ORIGINS` | 写完整 scheme+host+port，不能使用 `*` 配 Cookie |
| 上传 413/失败 | Nginx body size、multipart、目录权限 | 文件小于 2 MB、JPG/PNG、上传目录可写 |
| 更新后仍显示旧 JS | index.html 的 hash、缓存和镜像标签 | 重建 web 镜像并重新创建容器，硬刷新 |
| SQL 迁移语法错误或启动时报 RBAC 表缺失 | 客户端、执行顺序、表数 | 按 `medical.sql→V2→V3→V4→V5`；V2/V3/V5 需要 MySQL CLI；确认 23 张表 |
| `mvn verify` 覆盖率失败 | `target/site/jacoco` | 补测试或确认运行了准出命令 |
| systemd 启动后退出 | `journalctl -u medicine-backend` | 检查 JAR 路径、EnvironmentFile、JDK17、目录权限 |
| 反向代理刷新子路由 404 | Nginx `try_files`/SPA fallback | 使用仓库 Nginx 配置并确认站点根目录 |

历史证据目录中可能存在旧的 8080、18082 或 BusyBox 描述；当前根 `compose.yaml`、Dockerfile 和本 README 以 8082、9092、Nginx 同源代理为准。部分子 README 也保留了历史 token/端口说明，遇到冲突以源码、Compose 和本 README 为准。

## 16. 进一步文档

- `deploy/docker/README.md`：Docker Secret、Compose 操作和轮换细节。
- `deploy/README.md`：systemd、Nginx、目录布局和本地脚本。
- `medical-backend/README.md`：后端快速运行说明。
- `medical-managerment-system/README.md`：前端开发脚本。
- `api-tests/README.md`：Python、Postman、Newman 回归入口。
- `ci/codearts/README.md`：CodeArts 构建、部署和 TestPlan 烟测编排。
- `process-docs/开发工程师/`：需求、架构、数据库、API、构建部署和验收记录。
- `process-docs/开发工程师/07-rbac-technical-design.md`：V5 权限模型、运行时授权、迁移和威胁控制细节。
- `双仓库提交规范.md`：每次提交推送到 GitHub `main` 和 CodeArts `master` 的规则。
