# CI/CD 工程师

本目录归档 **CI/CD 工程师** 角色负责的过程文档与交付物。

## 角色职责

负责持续集成与持续部署（CI/CD）流水线的搭建、维护与发布物管理，确保代码从提交到上线的构建、测试、部署全程自动化、可追溯、可回滚；维护双仓库同步与部署健康检查。

## 已完成工作（基于 Git 历史）

| 时间 | 提交 | 内容 |
|---|---|---|
| 2026-07-13 | `34a3081` | 新增便携式 Docker Compose 运行时 |
| 2026-07-13 | `1d546d7` | localhost 与 secrets 零配置化 |
| 2026-07-13 | `8bac337` | 后端默认端口调整为 8082 |
| 2026-07-14 | `fc25003` | 新增 CodeArts CI/CD 流水线资产 |
| 2026-07-14 | `b5382f9` | CodeArts 流水线配置与 ci-build 脚本，修复 Maven 找不到 POM |
| 2026-07-14 | `f02ae5a` | 明确 API 健康等待策略 |
| 2026-07-14 | `9a9e60c` | 明确部署健康检查策略 |
| 2026-07-14 | `5b92d05` | compose 注入 cookie 配置并对齐 7d TTL |

## 交付物

- `ci/`：CodeArts 流水线配置与 `ci-build` 构建脚本
- `deploy/`：systemd 服务单元、nginx 配置、环境变量样例、部署与验证脚本
- `compose.yaml`：前后端一键构建与启动（`medicine-backend:1.0.0` + `medicine-web:1.0.0`）
- `双仓库提交规范.md`：origin（GitHub main）+ codearts（华为云 master）同步规范
- 部署健康证据：`process-docs/evidence/deployment/`

## 部署拓扑

- `medicine-web:1.0.0`：nginx:alpine 同源发布前端静态文件，对外 `127.0.0.1:9092`，反代 `/api` -> 后端
- `medicine-backend:1.0.0`：Temurin JRE 17，非 root（UID 10001），内部 8082，只读根文件系统
- MySQL/Redis 密码经 Docker Secrets 只读挂载，不进镜像或 Compose 明文
- 上传文件持久化于命名卷 `medicine_uploads`

> 角色专属的详细过程文档（构建记录、部署手册、回滚预案等）将按软件工程阶段补充至本目录。
