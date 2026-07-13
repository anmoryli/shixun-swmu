# 构建与部署记录

## 1. 构建结果

| 产物 | 命令 | 结果 |
|---|---|---|
| 前端 | `npm run build` | 成功，最终构建哈希 `7e83e2ecd52bfd75` |
| 后端测试 | `mvn clean test` | 8/8 通过 |
| 后端打包 | `mvn package` | 成功 |
| 后端 JAR | `medical-backend-1.0.0.jar` | 38,131,662 bytes |

Spring Boot 固定为 2.5.3，Spring Framework 实际解析为 5.3.10，编译目标为 Java 17。

## 2. JDK 17 实际运行门禁

本机原默认 JDK 为 21。为满足需求，不仅设置 `release=17`，还下载了被 Git 忽略的便携式 Temurin JDK 17，并用它启动最终 JAR：

```text
openjdk version "17.0.19"
Starting MedicalBackendApplication v1.0.0 using Java 17.0.19
Tomcat started on port(s): 8080
```

![部署健康门禁](evidence/deployment/deployment-health.png)

Actuator 最终状态：

```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP", "database": "MySQL"},
    "redis": {"status": "UP", "version": "7.0.15"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

## 3. 前后端联通

- 前端开发服务：[http://localhost:9092/](http://localhost:9092/)
- 后端健康检查：[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- 前端 `/api` 代理登录返回 `code=20000`。
- JDK 17 下登录、权限、仪表盘和退出均返回 `20000`。
- 前端开发服务 HTTP 200，生产 `dist/index.html` 存在。

当前两个服务均保持运行，便于验收查看。

## 4. openEuler/Linux 部署包

`deploy/` 已提供：

- `systemd/medicine-backend.service`
- `nginx/medicine.conf`
- `env/medicine-backend.env.example`
- `scripts/start-local.ps1`
- `scripts/verify-deployment.ps1`
- `README.md`

生产拓扑：Nginx 发布前端、同源代理 `/api`、单独映射 `/image`；后端以非 root 用户运行，密码来自权限为 0600 的 EnvironmentFile。

## 5. 部署验证与回滚

部署后验证：

1. `systemctl status medicine-backend`。
2. `curl http://127.0.0.1:8080/actuator/health`。
3. 管理员登录、菜单、仪表盘和八类 GET。
4. 医生写接口越权检查。
5. Nginx `/api` 和 `/image` 同源访问。

回滚：

1. 保留上一个 JAR 和前端目录。
2. 停止服务，替换为上一个 JAR/前端版本。
3. 仅当代码回滚要求时执行对应数据库回滚；原始 SQL 绝不可在有业务数据的 schema 重跑。
4. 重启并重复健康检查与登录烟测。

## 6. 当前部署边界

本次已完成当前 Windows 环境的 JDK 17 部署运行，并连接远程 MySQL/Redis。目标 IP 的 8080 已存在另一个受 Spring Security 保护的服务，且用户未提供操作系统 SSH 账号，因此没有覆盖远程主机现有服务；已交付可直接用于 openEuler 的 Nginx/systemd 配置。
