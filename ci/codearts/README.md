# CodeArts CI/CD 配置清单

本目录把项目映射为“构建和检查 -> 部署和测试”两阶段流水线。构建与检查并行；部署成功后再串行执行接口测试。

## 1. 流水线源

课程文档要求使用“华北-北京四”、CodeArts Repo 和 `master` 分支。流水线建议配置：

| 配置 | 值 |
|---|---|
| 名称 | `shixun-2-ci-cd` |
| 流水线源 | `Repo` |
| 代码仓 | `shixun-2` |
| 默认分支 | `master` |
| 触发方式 | 提交代码时触发，并保留手工执行 |

当前工作副本的分支是 `main`，项目规范记录的 CodeArts SSH 仓库是 `shixun-2`。若本机尚未配置该远端，按截图所用的 `master` 流水线源执行：

```bash
git remote add codearts git@codehub.devcloud.cn-north-4.huaweicloud.com:9b1957ad04f844cfa48e66dd371a59bf/shixun-2.git
git push -u codearts main:master
```

不要把 HTTPS 密码或 AK/SK 写入仓库。

## 2. “构建”任务

在“持续交付 > 编译构建”中新建任务 `构建`，代码源选择 Repo、仓库 `shixun-2`、分支 `master`。使用空白/自定义模板，按顺序添加：

1. Maven 构建：JDK `17`、Maven `3.9.x`，命令 `mvn -B -ntp -f medical-backend/pom.xml clean verify`。
2. npm 构建：Node.js `20`，命令 `npm --prefix medical-managerment-system ci --no-audit --no-fund && npm --prefix medical-managerment-system run build`。
3. Shell：命令 `CI_PACKAGE_ONLY=1 bash ci/codearts/build.sh`。
4. 上传软件包：上传路径 `ci-output/medicine-cicd.tar.gz`，软件包名称保持 `medicine-cicd.tar.gz`。

该任务会执行后端单元测试、构建 Vue 生产包，并生成一个不含数据库/Redis密码的部署包。

## 3. “代码检查”任务

在“代码 > 代码检查”中新建任务 `代码检查`：

- 仓库/默认分支：`shixun-2` / `master`。
- 语言启用 `Java`、`JavaScript`、`Python`（`api-tests/` 中有 Python 回归脚本）；关闭与本项目无关的语言。
- 规则集启用上述三种语言的安全、可靠性、可维护性规则。
- 流水线插件检查模式选择 `Full`。
- 在“设置 > 执行计划”开启定时执行，设为每天 `00:00`，满足课程统计要求。

## 4. 部署主机准备

目标 ECS 需要 Docker Engine、Docker Compose v2，并开放 TCP `9092`。一次性执行：

```bash
sudo install -d -m 755 /opt/medicine/releases /opt/medicine/incoming
sudo chown -R "$USER":"$USER" /opt/medicine
sudo install -d -m 700 -o "$USER" /etc/medicine/secrets
sudo cp ci/codearts/medicine-ci.env.example /etc/medicine/medicine-ci.env
sudo chown "$USER":"$USER" /etc/medicine/medicine-ci.env
sudo chmod 600 /etc/medicine/medicine-ci.env
sh -c 'umask 077; read -r MYSQL_PASSWORD; printf %s "$MYSQL_PASSWORD" > /etc/medicine/secrets/mysql-password.txt'
sh -c 'umask 077; read -r REDIS_PASSWORD; printf %s "$REDIS_PASSWORD" > /etc/medicine/secrets/redis-password.txt'
```

编辑 `/etc/medicine/medicine-ci.env`，填写真实的 MySQL/Redis 地址及最小权限数据库账号。两个密码文件只保留在 ECS，不上传 CodeArts、不写入环境变量。

将 CodeArts 部署主机使用的账号加入 `docker` 组并重新登录，使部署步骤无需 `sudo` 执行 Docker。Docker 组等同高权限，仅授予专用部署账号。

## 5. “部署”任务

在“持续交付 > 部署”中新建空白应用 `部署`，绑定已纳管的 ECS 环境：

1. 添加“选择部署来源”，源类型选“构建任务”，任务选上面的 `构建`，下载目录填 `/opt/medicine/incoming`。
2. 添加“执行 Shell 命令”，执行：

```bash
set -Eeuo pipefail
artifact="$(find /opt/medicine/incoming -type f -name medicine-cicd.tar.gz -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)"
test -n "$artifact"
tar -xOf "$artifact" ./codearts/deploy.sh | bash -s -- "$artifact" /opt/medicine
```

部署脚本使用版本目录、Docker 健康检查和 `current` 软链接；新版本不健康时会尝试恢复上一个镜像版本。

## 6. “接口测试”任务

推荐使用 CodeArts TestPlan 原生接口测试，便于教师端统计：

1. 在“测试 > 测试用例 > 接口自动化”导入 `api-tests/postman/medicine-codearts-smoke.postman_collection.json`（Postman Collection v2.1）。
2. 建立环境参数 `baseUrl`、`adminUsername`、`adminPassword` 和动态参数 `adminToken`。引用形式按 TestPlan 规则改为 `$${baseUrl}` 等；`baseUrl` 填 `http://<ECS公网IP>:9092`，用户名和密码勾选敏感信息脱敏。
3. TestPlan 的 Postman 导入只迁移请求，不迁移 JavaScript 断言。给 `01 Health` 添加 HTTP `200` 和响应 JSON `$.status == UP` 检查点；给其余步骤添加 HTTP `200`、`$.code == 20000` 检查点。
4. 在 `02 Admin Login` 增加响应提取，把 `$.data.token` 赋值给动态参数 `adminToken`；后续三个请求的 `Authorization` 请求头引用 `$${adminToken}`。
5. 创建串行测试套件 `接口测试`，按 `01` 到 `05` 的顺序加入五个步骤。
6. 在流水线“部署和测试”阶段中，先添加 `Deploy部署`，再在它下面以串行方式添加 `TestPlan接口测试`，任务类型选择“接口自动化”，套件选择 `接口测试`。

若当前套餐不能使用 TestPlan，可在部署主机或自定义执行机运行备用脚本：

```bash
BASE_URL=http://<ECS公网IP>:9092 \
ADMIN_USERNAME='<私密参数>' ADMIN_PASSWORD='<私密参数>' \
bash ci/codearts/api-test.sh
```

## 7. 最终编排

```text
流水线源(master)
  -> 构建和检查
       |- 构建（并行）
       `- 代码检查（并行，Full）
  -> 部署和测试
       |- 部署
       `- 接口测试（串行依赖部署）
```

首次保存后手工执行一次。通过标准是：构建包上传成功、代码检查完成、两个容器为 healthy、`/actuator/health` 返回 HTTP 200、接口套件全部通过。

## 8. 官方参考

- [通过流水线生成软件包并部署到主机](https://support.huaweicloud.com/qs-pipeline/pipeline_qs_0000.html)
- [配置代码检查任务定时执行](https://support.huaweicloud.com/usermanual-codecheck/codecheck_01_0005.html)
- [Check 代码检查流水线插件](https://support.huaweicloud.com/usermanual-pipeline/pipeline_01_0084.html)
- [TestPlan 接口测试流水线插件](https://support.huaweicloud.com/usermanual-pipeline/pipeline_01_0087.html)
