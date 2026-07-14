# Docker Compose 部署

先确认 Docker Engine 与 Compose 可用：

```powershell
docker info
docker compose version
```

## 1. 密码配置

MySQL 与 Redis 密码使用 Docker Secrets 文件，不写入 `compose.yaml`、镜像或 Git。首次运行执行：

```powershell
.\deploy\docker\init-secrets.ps1
```

如果旧版本曾把 `deploy/docker/secrets/*-password.txt` 推送到远端，请在数据库和 Redis 服务端轮换对应密码；仅从最新提交删除文件不能清除 Git 历史中的旧值。

脚本会安全提示输入密码，并创建两个被 Git 忽略的本机文件：

- `.work/private/docker/mysql-password.txt`
- `.work/private/docker/redis-password.txt`

Linux/openEuler 可使用隐藏输入创建同样的文件：

```bash
install -d -m 700 .work/private/docker
read -rsp 'MySQL password: ' MYSQL_PASSWORD; echo
printf '%s' "$MYSQL_PASSWORD" > .work/private/docker/mysql-password.txt
unset MYSQL_PASSWORD
read -rsp 'Redis password: ' REDIS_PASSWORD; echo
printf '%s' "$REDIS_PASSWORD" > .work/private/docker/redis-password.txt
unset REDIS_PASSWORD
chmod 600 .work/private/docker/*-password.txt
```

`init-secrets.ps1` 只写本地客户端 Secret，不会修改远程 MySQL/Redis 的服务端密码。轮换时必须先在远程服务端修改对应密码，再执行以下命令更新本地同一项：

```powershell
.\deploy\docker\init-secrets.ps1 -Target MySQL -Force
# 或：.\deploy\docker\init-secrets.ps1 -Target Redis -Force
docker compose up -d --force-recreate backend web
docker compose ps
curl.exe -f http://localhost:8082/actuator/health
```

只有后端恢复 `healthy` 且登录验证成功，密码轮换才算完成；失败时需同时回滚远程服务端密码和本地 Secret。轮换前应备份现有 Secret 到仓库外的安全位置。

非敏感配置可在启动前用环境变量覆盖：`MEDICINE_DB_URL`、`MEDICINE_DB_USERNAME`、`MEDICINE_REDIS_HOST`、`MEDICINE_REDIS_PORT`、`MEDICINE_REDIS_DATABASE`、`MEDICINE_TOKEN_TTL`、`MEDICINE_CORS_ALLOWED_ORIGINS`、`MEDICINE_BIND_ADDRESS`、`MEDICINE_WEB_PORT`、`MEDICINE_IMAGE_TAG`、`MEDICINE_PROJECT_NAME`、`MEDICINE_UPLOAD_VOLUME`、`MEDICINE_NETWORK_NAME` 以及前后端资源上限变量。

## 2. 构建并启动

在仓库根目录运行：

```powershell
docker compose up -d --build
docker compose ps
```

Windows 首次运行还应确认 Docker Desktop 使用 Linux 容器，并检查远程依赖和端口：

```powershell
docker context use desktop-linux
docker info
Test-NetConnection 106.54.210.109 -Port 3306
Test-NetConnection 106.54.210.109 -Port 6379
docker compose config --quiet
```

若 PowerShell 执行策略阻止初始化脚本，可仅对该次命令使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\docker\init-secrets.ps1
```

访问地址：

- 系统页面：<http://localhost:9092/>
- 后端健康检查：<http://localhost:8082/actuator/health>

后端容器和宿主机默认都使用 `8082`；前端使用 BusyBox 静态服务发布页面，不包含 Nginx 配置。浏览器直接通过 `localhost:8082` 调用 API。

默认只绑定 `127.0.0.1:9092`，局域网设备无法访问。确需局域网演示时应先修改默认管理员密码，再显式执行：

```powershell
$env:MEDICINE_BIND_ADDRESS = '0.0.0.0'
docker compose up -d
```

若 9092 被占用，可设置 `$env:MEDICINE_WEB_PORT='9093'` 后启动。

## 3. 验证

```powershell
docker compose ps
curl.exe http://localhost:9092/
curl.exe http://localhost:8082/actuator/health

$env:BASE_URL = 'http://localhost:8082'
$env:ADMIN_USERNAME = '<管理员账号>'
$env:ADMIN_PASSWORD = '<管理员密码>'
python .\api-tests\run_api_tests.py
```

## 4. 日志、停止和升级

```powershell
docker compose logs -f --tail 100
docker compose stop
docker compose start
docker compose down
docker compose up -d --build
```

构建新版本前可给当前镜像添加备份标签；回滚时通过 `MEDICINE_IMAGE_TAG` 选择已保留的标签并使用 `--no-build`：

```powershell
docker tag medicine-backend:1.0.0 medicine-backend:backup-20260713
docker tag medicine-web:1.0.0 medicine-web:backup-20260713
$env:MEDICINE_IMAGE_TAG = 'backup-20260713'
docker compose up -d --no-build --force-recreate
```

上传图片保存在命名卷 `medicine_uploads`，普通 `docker compose down` 不会删除。除非明确需要清空上传数据，否则不要执行 `docker compose down -v`。

## 5. 安全边界

- 当前远程 MySQL 使用 root 账号且连接关闭 SSL，Redis 也未启用 TLS；生产环境应创建最小权限账号并使用私网/VPN/TLS。
- 原始 SQL 含建表和删除语句，容器启动不会自动重复导入数据库。
- Docker Secrets 在单机 Compose 中以只读文件挂载，避免出现在镜像和 `docker inspect` 环境变量中；生产集群应改用平台级 Secret 管理。
- API 回归每次会在 `medicine_uploads` 中留下一个 1×1 PNG；长期测试时应定期按测试报告中的 URL 清理无用文件。
- 原始 SQL 的 6 条药品图片地址仍指向历史 `localhost:8080`，且对应原始文件不在仓库；新上传图片与 Docker 持久卷访问正常。
