# Docker Compose 部署实测证据（2026-07-13）

## 环境与镜像

- Docker Engine：29.4.1
- Docker Compose：v5.1.3
- 后端镜像：`medicine-backend:1.0.0`（本次 Compose 构建）
- 前端镜像：`medicine-web:1.0.0`（BusyBox 静态运行时）
- 后端运行时：Eclipse Temurin 17.0.19+10
- 前端运行时：BusyBox httpd 1.36.1-musl

## 安全门禁

- 后端容器用户：`10001:10001`
- 后端根文件系统：只读
- `no-new-privileges`：已启用
- Linux capabilities：全部移除
- 默认资源上限：后端 768 MB / 1.5 CPU / PID 256；前端 128 MB / 0.5 CPU / PID 128
- 默认端口绑定：仅 `127.0.0.1:9092`
- 日志轮转：10 MB × 3 文件
- 镜像元数据和历史真实密码扫描：通过
- 容器环境真实密码扫描：通过
- 密码来源：Git 忽略且 ACL 收紧的 Docker Secrets 文件

## 运行验证

| 验证项 | 结果 |
|---|---|
| `docker compose config --quiet` | 通过 |
| 后端镜像内 Maven `clean verify` | 8/8 |
| `medicine-backend-1` | healthy |
| `medicine-web-1` | healthy |
| `http://localhost:9092/` | HTTP 200 |
| `http://localhost:18082/actuator/health` | HTTP 200 / UP |
| API 黑盒回归（后端直连） | 57/57 |
| 上传 URL 直接访问后端 | `localhost:18082/image/...` |
| `docker compose down` 后重新创建 | 上传图片仍 HTTP 200 |

API 明细证据：`../api/api-test-20260713T065014Z.{json,xml,md}`。
