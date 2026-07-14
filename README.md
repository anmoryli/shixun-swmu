# 慧医数字医疗应用系统

基于 Vue 3、Spring Boot 2.7.18、Spring Security、MyBatis、MySQL 和 Redis 的前后端分离医疗基础数据管理系统。

## 目录

- `medical-managerment-system/`：Vue 3 + Vite 前端
- `medical-backend/`：Spring Boot 后端
- `sql/`：原始 SQL 与增量迁移
- `api-tests/`：Python 黑盒回归和 Postman Collection
- `deploy/`：Docker、systemd、环境模板和部署脚本
- `process-docs/`：独立的全流程文档与证据

## 当前访问

- Docker 前端：<http://localhost:9092/>
- Docker 同源健康检查：<http://localhost:9092/actuator/health>

## Docker 一键运行

首次运行先配置本机 Docker Secrets，然后启动：

```powershell
.\deploy\docker\init-secrets.ps1
docker compose up -d --build
```

已经创建过私有密码文件时可直接执行第二条命令。完整说明见 `deploy/docker/README.md`。

## 构建

```powershell
cd medical-backend
mvn clean test package

cd ..\medical-managerment-system
npm install
npm run build
```

远程数据库和 Redis 密码必须通过本机私有环境文件或 Docker Secrets 传入，不要写入仓库。完整启动、测试和部署方法见 `process-docs/` 与 `deploy/README.md`。

## CodeArts CI/CD

CodeArts 的“构建、代码检查、部署、接口测试”任务参数与仓库内脚本见 [`ci/codearts/README.md`](ci/codearts/README.md)。
