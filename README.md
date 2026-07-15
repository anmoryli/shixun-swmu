# 慧医数字医疗应用系统

慧医数字医疗应用系统是一个前后端分离的医疗基础数据管理平台，面向药品、医生、医药公司、政策、销售地点、城市和资料等业务数据提供统一的管理界面。项目由 Vue 3 管理端、Spring Boot API、MySQL 业务库和 Redis 会话/缓存组成。

本文是项目的主运行手册，优先给出仓库当前配置可以直接执行的命令。涉及密码、数据库地址、地图 Key 的位置只使用占位符；真实值必须放在本机或服务器的私有配置中，不能提交到 Git。

## 0. 先选择部署方式

| 场景 | 推荐方式 | 前端入口 | 后端健康检查 |
| --- | --- | --- | --- |
| Windows 11/10 本地演示 | Docker Desktop + Compose | http://localhost:9092/ | http://localhost:8082/actuator/health |
| macOS 本地演示 | Docker Desktop + Compose | http://localhost:9092/ | http://localhost:8082/actuator/health |
| Ubuntu/openEuler 服务器 | Docker Engine + Compose v2 | http://<服务器>:9092/（也可放到 80/443 反向代理后） | http://<服务器>:9092/actuator/health |
| Windows 原生开发 | JAR + Vite 开发服务器 | http://localhost:9092/#/user/login | http://localhost:8082/actuator/health |
| Linux/openEuler 原生生产 | JAR + systemd + Nginx | http://<域名>/ | http://<域名>/actuator/health |
| macOS 原生运行 | JAR + Nginx/Caddy 或前台进程 | 按本机反向代理地址 | http://127.0.0.1:8082/actuator/health |
| 华为云 CodeArts ECS | ci/codearts 中的构建、部署和烟测流水线 | 由部署环境变量决定 | MEDICINE_WEB_PORT 对应的 /actuator/health |

除 Docker 镜像和 Compose 网络外，项目不会自动创建 MySQL 或 Redis。无论选择哪种方式，都必须先准备可连接的 MySQL 8 和 Redis 7，并完成数据库初始化。

## 1. 项目组成与运行边界

~~~text
根目录
├─ medical-managerment-system/  Vue 3 + Vite + Vue Router + Vuex + Element Plus
├─ medical-backend/             Spring Boot 2.7.18 + Spring Security + MyBatis
├─ sql/                         基础 schema/种子数据与 V2、V3、V4 增量迁移
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
~~~

PowerShell 不建议直接使用 `<` 重定向；可通过 `cmd.exe` 调用 MySQL 客户端：

~~~powershell
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\medical.sql"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\migrations\V2__requirements_alignment.sql"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\migrations\V3__password_audit_delete_policy.sql"
cmd /c "mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p medicine < sql\migrations\V4__sale_map_menu.sql"
~~~

执行顺序必须是 `medical.sql → V2 → V3 → V4`。V2/V3 含 `DELIMITER`，应使用 MySQL 命令行客户端，不要用只支持单条语句的普通 SQL 面板逐句粘贴。V4 没有依赖当前连接默认库的保险设置，命令中应显式指定 `medicine`。

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
   docker compose config --quiet
   ~~~

2. 首次创建本机 Docker Secrets：

   ~~~powershell
   powershell -ExecutionPolicy Bypass -File .\deploy\docker\init-secrets.ps1
   ~~~

   脚本会交互输入 MySQL、Redis 密码，生成被 Git 忽略的 `.work\private\docker\mysql-password.txt` 和 `redis-password.txt`。如果远程密码已经轮换，才使用 `-Target MySQL -Force` 或 `-Target Redis -Force` 更新对应文件。

3. 启动并查看状态：

   ~~~powershell
   docker compose up -d --build
   docker compose ps
   curl.exe -f http://localhost:9092/actuator/health
   curl.exe -f http://localhost:8082/actuator/health
   ~~~

4. 浏览器访问 `http://localhost:9092/`，登录页路由为 `http://localhost:9092/#/user/login`。

远程数据库/Redis 不在 Docker Desktop 内时，先用 `Test-NetConnection <host> -Port 3306` 和 `Test-NetConnection <host> -Port 6379` 检查可达性，再通过 Compose 环境变量覆盖默认地址。

### 4.3 Linux/openEuler 和 macOS Docker Desktop

Docker 命令相同；Linux 需要先安装 Docker Engine 和 Compose v2，macOS 需要启动 Docker Desktop。首次创建 POSIX Secret：

~~~bash
mkdir -p .work/private/docker
chmod 700 .work/private/docker
umask 077
read -r -s -p 'MySQL password: ' MYSQL_PASSWORD; printf '\n'
printf '%s' "$MYSQL_PASSWORD" > .work/private/docker/mysql-password.txt
unset MYSQL_PASSWORD
read -r -s -p 'Redis password: ' REDIS_PASSWORD; printf '\n'
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
| `MEDICINE_CORS_ALLOWED_ORIGINS` | localhost:9092 两个来源 | 精确允许的前端 Origin，不能用 `*` 配合 Cookie |
| `MEDICINE_COOKIE_SECURE` | `false` | HTTPS 生产设为 `true` |
| `MEDICINE_AMAP_WEB_KEY` | 空 | 仅后端持有的高德 Web 服务 Key |
| `MEDICINE_API_BASE_URL` | `/api` | 前端构建参数，推荐保持同源 `/api` |
| `MEDICINE_BIND_ADDRESS` / `MEDICINE_WEB_PORT` | `127.0.0.1` / `9092` | Web 宿主绑定和端口 |
| `MEDICINE_BACKEND_BIND` / `MEDICINE_BACKEND_PORT` | `127.0.0.1` / `8082` | 后端宿主绑定和端口 |
| `MEDICINE_IMAGE_TAG` | `1.0.0` | 镜像标签 |
| `MEDICINE_UPLOAD_VOLUME` | `medicine_uploads` | 上传持久卷名称 |

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

产物为 `dist/`。前端地图需要 `VITE_AMAP_JS_KEY` 和 `VITE_AMAP_SECURITY_CODE`。先复制被忽略的本地环境文件并填写受域名限制的 JS Key；高德 Web 服务 Key 不要放入 `VITE_*`，只通过后端 `AMAP_WEB_KEY` 注入。没有地图 Key 时主系统仍可运行，但地图功能不可用。

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
sudo journalctl -u medicine-backend -f
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

## 7. 进一步文档

- `deploy/docker/README.md`：Docker Secret、Compose 操作和轮换细节。
- `deploy/README.md`：systemd、Nginx、目录布局和本地脚本。
- `medical-backend/README.md`：后端快速运行说明。
- `medical-managerment-system/README.md`：前端开发脚本。
- `api-tests/README.md`：Python、Postman、Newman 回归入口。
- `ci/codearts/README.md`：CodeArts 构建、部署和 TestPlan 烟测编排。
- `process-docs/开发工程师/`：需求、架构、数据库、API、构建部署和验收记录。
- `双仓库提交规范.md`：每次提交推送到 GitHub `main` 和 CodeArts `master` 的规则。

