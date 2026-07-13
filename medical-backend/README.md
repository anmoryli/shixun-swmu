# medical-backend

慧医数字医疗应用系统的 Spring Boot 后端。默认监听 `8082`，API 前缀为 `/api`。

## 运行环境

- JDK 17
- Maven 3.8+
- MySQL 8（schema：`medicine`）
- Redis 7

远程服务密码不写入仓库，启动前通过环境变量注入：

```powershell
$env:DB_PASSWORD = '<MySQL 密码>'
$env:REDIS_PASSWORD = '<Redis 密码>'
mvn spring-boot:run
```

可覆盖的主要变量包括 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、
`REDIS_PORT`、`REDIS_PASSWORD`、`SERVER_PORT`、`TOKEN_TTL` 与 `CORS_ALLOWED_ORIGINS`。

## 认证接口

- `POST /api/login`：同时支持表单和 JSON 登录；返回前端兼容的裸 Token 与数字 `utype`。
- `GET /api/permissions`：以 Redis 会话中的角色查询权限树，不信任客户端传入的角色。
- `POST /api/logout`：删除当前 Redis 会话。
- `GET /actuator/health`：无需登录的健康检查。

运行测试：`mvn test`。
