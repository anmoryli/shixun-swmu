# 慧医数字医疗应用系统

基于 Vue 2、Spring Boot 2.5.3、Spring Security、MyBatis、MySQL 和 Redis 的前后端分离医疗基础数据管理系统。

## 目录

- `medical-managerment-system/`：Vue 2 前端
- `medical-backend/`：Spring Boot 后端
- `sql/`：原始 SQL 与增量迁移
- `api-tests/`：Python 黑盒回归和 Postman Collection
- `deploy/`：Nginx、systemd、环境模板和部署脚本
- `process-docs/`：独立的全流程文档与证据

## 当前访问

- 前端：<http://localhost:9092/>
- 后端健康检查：<http://localhost:8082/actuator/health>

## 构建

```powershell
cd medical-backend
mvn clean test package

cd ..\medical-managerment-system
npm install
npm run build
```

远程数据库和 Redis 密码必须通过环境变量传入，不要写入仓库。完整启动、测试和部署方法见 `process-docs/` 与 `deploy/README.md`。
