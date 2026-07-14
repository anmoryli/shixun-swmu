# CodeArts CI/CD 全过程记录

## 1. 最终结果

2026-07-14，项目在华北-北京四的 CodeArts 项目中完成了从代码仓库到 ECS 的完整流水线。最终通过记录为流水线 **#12**，提交 `41ade53e`，总耗时 **5 分 1 秒**。

| 阶段 | 任务 | 结果 | 耗时 |
|---|---|---|---|
| 构建和检查 | 构建1 | 成功 | 2 分 22 秒 |
| 构建和检查 | 代码检查 | 成功 | 1 分 21 秒 |
| 部署和测试 | 部署 | 成功 | 1 分 41 秒 |
| 接口测试 | 执行 Shell | 成功 | 56 秒 |

最终执行顺序如下：

```text
CodeArts Repo: shixun-2 / master
  -> 构建和检查
       |- 构建1（并行）
       `- 代码检查（并行）
  -> 部署和测试
       `- 部署到 shixun-prod
  -> 接口测试
       `- 检查公网 /actuator/health
```

接口测试单独放在部署之后，确保验证的是刚刚发布的新版本，而不是部署前仍在运行的旧版本。

## 2. 流水线源

流水线使用以下代码源：

| 项目 | 配置 |
|---|---|
| CodeArts Repo | `shixun-2` |
| 默认分支 | `master` |
| 流水线名称 | `anmory-20260714110641` |
| 区域 | 华北-北京四 |

本次验收运行由页面手工触发。若需要持续集成自动执行，应在“流水线 > 编辑 > 触发设置”中开启代码提交触发，并只监听 `master`。这样功能分支上的提交不会直接部署，PR 合并到 `master` 后才会自动执行完整流水线。

## 3. 构建任务

构建任务从 `master` 拉取代码，执行以下工作：

1. 使用 JDK 17 和 Maven 构建后端并运行单元测试。
2. 使用 Node.js 构建 Vue 前端生产资源。
3. 执行 `ci/codearts/build.sh` 生成部署包。
4. 上传 `ci-output/medicine-cicd.tar.gz` 作为流水线制品。

制品包含部署所需的 Compose 文件、前后端 Docker 构建上下文和 CodeArts 部署脚本，但不包含数据库或 Redis 密码。

## 4. 代码检查

代码检查与构建并行运行，检查 Java、JavaScript 和 Python 代码。两个任务都成功后才允许进入部署阶段。并行方式缩短了流水线总时间，同时保留了构建失败和代码质量失败的独立结果。

## 5. ECS 与部署环境

CodeArts 已纳管目标环境 `shixun-prod`，本次部署到公网地址 `1.14.150.130` 的 ECS，SSH 端口为 22。应用对外发布端口为 9092。

服务器上的主要目录为：

| 路径 | 用途 |
|---|---|
| `/opt/medicine/incoming` | CodeArts 下载构建制品 |
| `/opt/medicine/releases` | 按构建号保存版本目录 |
| `/opt/medicine/current` | 指向当前健康版本的软链接 |
| `/etc/medicine/medicine-ci.env` | 非敏感运行配置 |
| `/etc/medicine/secrets/mysql-password.txt` | MySQL Docker Secret |
| `/etc/medicine/secrets/redis-password.txt` | Redis Docker Secret |

`medicine-ci.env` 只记录数据库地址、用户名、Redis 地址、端口等非敏感配置。密码通过 CodeArts 私密参数写入 ECS 的 Secret 文件，不写入 Git、Compose 文件、镜像或普通环境变量。

部署应用使用以下步骤：

1. 修复目标机 Python HTTPS 依赖。
2. 从构建任务下载 `medicine-cicd.tar.gz`。
3. 准备 `/etc/medicine` 配置和 Secret 文件。
4. 执行制品内的 `ci/codearts/deploy.sh`。
5. 使用 Docker Compose 构建并启动后端和 Web 容器。
6. 等待容器健康，并访问 ECS 本机的 `/actuator/health`。
7. 健康后更新 `/opt/medicine/current`；失败时尝试恢复上一个版本。

后端容器以 UID/GID 10001 运行，因此 Secret 文件需要对该账号可读，同时禁止其他用户读取。项目部署脚本使用只读挂载将密码传入容器。

## 6. MySQL 与 Redis 配置

数据库使用独立 schema `medicine`，应用使用最小权限数据库账号，不使用数据库服务器的 root 账号运行应用。CodeArts 中使用的敏感参数只有参数名被记录，真实值不得进入文档：

- `mysqlpassword`
- `redispassword`

本次调试中曾出现两类连接错误：

1. MySQL root 远程认证失败：改为仅拥有 `medicine.*` 权限的应用账号。
2. Redis 返回 `WRONGPASS`：重新保存 CodeArts Redis 私密参数，并由“准备 medicine 配置”步骤覆盖 ECS Secret 文件。

私密参数更新后，第 11 次流水线的部署任务成功，证明 MySQL、Redis、容器健康检查和版本发布链路已经连通。

## 7. 接口测试方案

最初选择了 CodeArts TestPlan 接口测试插件，但当前项目没有开通 TestPlan 高阶接口自动化能力，插件没有可选择的任务。空任务被保存为未解析的 `jobId`，运行时出现：

```text
Illegal character in path ... /suites/${task.state.extendProperties.config.parameters.jobId}/start
```

最终删除不可用的 TestPlan 任务，新增独立“接口测试”阶段，并使用 CodeArts 官方“执行 Shell”插件验证公网服务：

```bash
set -Eeuo pipefail
url="http://1.14.150.130:9092/actuator/health"
body="$(curl --fail --silent --show-error \
  --retry 12 --retry-delay 5 --retry-connrefused "$url")"
printf '%s\n' "$body"
printf '%s' "$body" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'
```

该检查同时验证：

- ECS 公网 9092 端口可访问；
- Nginx/Web 到后端的代理正常；
- Spring Boot 已启动；
- Actuator 总体状态为 `UP`。

如果后续开通 TestPlan，可在保留健康检查的基础上，再增加登录、Token 提取、菜单、仪表盘和退出等业务接口回归。

## 8. 故障与处理记录

| 执行 | 现象 | 根因 | 处理 |
|---|---|---|---|
| #6 | Secret 文件不可读 | 容器以 10001 运行，文件权限不匹配 | 调整 Secret 所有者和只读权限 |
| #7 | 数据库/Redis 连接失败 | root 远程认证与 Redis 密码不匹配 | 创建最小权限数据库账号，更新私密参数 |
| #9 | 部署插件报参数为空 | 流水线部署插件要求运行时参数非空 | 设置环境、制品占位参数和关联构建任务 |
| #10 | Redis `WRONGPASS` | ECS Secret 中的 Redis 密码不正确 | 重新写入并保存 Redis 私密参数 |
| #11 | TestPlan URL 含未解析 `jobId` | 项目未开通接口自动化任务 | 改为部署后的 Shell 健康检查 |
| #12 | 全流程通过 | 配置修复完成 | 作为最终验收记录 |

## 9. 日常使用方式

推荐协作流程：

1. 成员在各自功能分支开发并提交 PR。
2. PR 评审通过后合并到 `master`。
3. CodeArts 监听 `master` 的提交并启动流水线。
4. 构建和代码检查必须全部成功。
5. 制品部署到 `shixun-prod` ECS。
6. 公网健康检查成功后，本次发布完成。

多人协作时不要直接向 `master` 强制推送，也不要让功能分支自动部署生产 ECS。若发布失败，应先查看失败任务日志；不要反复修改服务器而不把稳定配置同步回 CodeArts 部署应用。

## 10. 安全与运维注意事项

- 不在仓库、PR、截图、日志或文档中保存真实密码。
- CodeArts 密码参数必须设置为私密参数。
- 应用数据库账号只授予 `medicine.*` 所需权限。
- ECS 安全组仅开放实际需要的 22 和 9092，并限制 SSH 来源。
- SSH 建议改为密钥登录并关闭 root 密码远程登录。
- 密码轮换后同步更新 CodeArts 私密参数，再执行一次完整流水线。
- 保留 `/opt/medicine/releases` 中最近几个版本，以便回滚，同时定期清理旧镜像和旧制品。

