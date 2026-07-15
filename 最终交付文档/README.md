# 慧医数字医疗应用系统 — 最终交付文档

## 本次新增交付

| 文件 | 内容 |
|---|---|
| `慧医数字医疗应用系统_完整实习设计报告.docx` | 53 页完整实习设计报告；含需求分析、概要设计、详细设计、测试结果、部署与持续集成、总结及附录；嵌入图片目录全部 47 张 PNG；待填写项已黄色高亮 |

> 本文件是实训项目的**交付总览与索引**，汇总系统交付物、验收结论、仓库结构与运行方式。详细过程文档见 `process-docs/`。

## 一、项目概述

慧医数字医疗应用系统是一套前后端分离的医疗基础数据管理系统，面向**管理员**（ROLE_1，全量维护）与**医生**（ROLE_2，业务只读）两类角色，覆盖登录授权、首页数据面板、医生信息、医药公司、销售地点、城市、药品、医保政策、公司政策、必备材料共八个业务模块，并包含药品图片安全上传与销售地点经纬度坐标扩展。

项目以现有 Vue 2 前端、需求规格说明书与 `sql/medical.sql` 为基线，补齐 Spring Boot 后端，连接远程 MySQL/Redis，完成数据库导入、接口回归、前后端联调与部署验证，并保留可复核的过程文档与证据。

## 二、交付结论

系统已从“仅有前端、后端接口缺失”补齐为可运行的前后端系统，全部验收项通过：

| 指标 | 结果 |
|---|---|
| Maven 单元/集成测试 | 8/8 |
| 黑盒 API 回归 | 57/57（平均 309.9 ms，P95 550 ms） |
| 前端生产构建 | 通过 |
| JDK 17 启动 | 通过 |
| Docker Compose | 2/2 容器健康，同源 API 57/57 |
| MySQL / Redis 健康检查 | UP / UP |
| 临时测试数据清理 | 0 条残留 |

## 三、仓库结构与交付物索引

下列路径均相对仓库根目录。

| 路径 | 交付内容 |
|---|---|
| `medical-managerment-system/` | Vue 2 前端：登录、动态路由、仪表盘、八类管理页面、销售地点坐标 |
| `medical-backend/` | Spring Boot 2.7.18 后端：认证、权限、Redis Token、42 个唯一 API、图片上传、健康检查 |
| `sql/` | `medical.sql` 原始 17 表 + V2/V3 迁移（共 20 张表） |
| `api-tests/` | Python 黑盒回归脚本 + Postman Collection（57/57 通过） |
| `deploy/` | Docker Compose、systemd、Nginx、环境变量模板、部署与验证脚本 |
| `compose.yaml` | 前后端一键构建与启动（`medicine-backend:1.0.0` + `medicine-web:1.0.0`） |
| `uml-diagrams/` | 18 张 UML 图（`.puml` 源 + 渲染 PNG） |
| `process-docs/` | 全流程文档与证据（开发 / CI-CD / 测试 / 项目管理） |
| `ci/codearts/` | CodeArts 流水线配置与 `ci-build` 构建脚本 |
| `测试/` | 测试用例与执行 xlsx 工作簿（8 份） |
| `代码检查/` | CodeArts 代码扫描报告 PDF（3 份） |
| `需求/` | 10 个需求演示视频（mp4） |
| `慧医数字医疗应用系统_需求规格说明书.docx` | 需求规格说明书 |
| `CodeArts使用文档（组长）.pdf` | CodeArts 平台使用手册 |
| `双仓库提交规范.md` | origin（GitHub main）+ codearts（华为云 master）同步规范 |
| `README.md` | 顶层项目说明与运行入口 |

## 四、技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 2、Vue Router（动态路由）、Vuex、Element UI、axios、ECharts、高德地图 |
| 后端 | Spring Boot 2.7.18、Spring Security、Spring MVC、MyBatis、JDK 17（不使用 Lombok） |
| 数据库 | MySQL 8（schema `medicine`，20 张表） |
| 缓存 | Redis 7（登录会话 + 权限，Token 以 SHA-256 摘要存储，不存明文，TTL 30 分钟） |
| 部署 | Docker Compose、Nginx 同源发布、Docker Secrets、systemd |

## 五、快速运行（Docker 一键）

首次运行先配置本机 Docker Secrets，再启动：

```powershell
.\deploy\docker\init-secrets.ps1
docker compose up -d --build
```

启动后访问：

- 前端：<http://localhost:9092/>
- 健康检查：<http://localhost:9092/actuator/health>

> 每次改动代码后需 `docker compose up -d --build` 重建，固定 tag `1.0.0` 才会更新。完整说明见 `deploy/docker/README.md` 与 `process-docs/开发工程师/05-build-and-deployment.md`。

## 六、验收结果

| 验收项 | 结果 |
|---|---|
| 需求和 SQL 追踪 | 通过 |
| MySQL 原始 SQL 导入 | 通过 |
| Redis 连接 | 通过 |
| 管理员/医生权限 | 通过 |
| 八类 CRUD | 通过 |
| 图片安全上传 | 通过 |
| 首页数据面板 | 通过 |
| 销售地点坐标 | 通过 |
| Maven 测试 | 8/8 |
| API 回归 | 57/57 |
| 前端生产构建 | 通过 |
| JDK 17 启动 | 通过 |
| Docker Compose | 2/2 healthy，API 57/57 |
| MySQL/Redis 健康检查 | UP/UP |
| 临时测试数据清理 | 0 条残留 |

详见 `process-docs/开发工程师/06-acceptance-summary.md`。

## 七、过程文档地图（process-docs/）

| 目录 | 内容 |
|---|---|
| `开发工程师/` | 00 全流程跟踪、01 需求基线、02 架构与设计、03 数据库实现、04 接口参考与测试、05 构建与部署、06 验收总结；另含前端/后端/系统开发工程师三份角色文档 |
| `CICD工程师/` | README + 07 CodeArts CI/CD 全过程记录（提交→构建→检查→制品→部署→接口验证） |
| `测试工程师/` | README：回归结果与缺陷三轮闭环记录 |
| `project-management/` | Epic/Feature/Story/Task/里程碑 CSV + 57 项黑盒场景 + ProjectMan 录入说明 |
| `evidence/` | api、deployment、requirements 证据目录 |

## 八、UML 设计图

`uml-diagrams/` 下共 18 张图，覆盖软件工程三阶段，源文件为 `.puml`，均附渲染 PNG：

- **需求分析**：用例图
- **静态建模**：领域类图、后端设计类图、ER 图、对象图、组件图、包图、部署图
- **动态建模**：活动图 ×3、状态图 ×2、顺序图 ×5

清单与渲染方式见 `uml-diagrams/README.md`。

## 九、团队与计划

| 成员 | 角色 | 分工 |
|---|---|---|
| anmory666 | 项目经理/开发 | 认证与后端基础 |
| Ning_24 | CI/CD 工程师 | 仪表盘、基础资料、流水线 |
| khyxone | 开发 | 医生、药品 |
| spikezzw | 开发 | 材料、政策、上传 |
| mengyetianxing | 测试 | 前端联调、测试交付 |

计划周期：2026-07-13 至 2026-07-17，共 5 个工作日。工作项分层（Epic→Feature→Story→Task）见 `process-docs/project-management/`。

## 十、已知边界与安全说明

**已知边界：**

- 当前浏览器控制环境无可用实例，未自动生成登录后页面截图；前端 HTTP、代理登录与 API 联调均已通过。
- 高德地图密钥未入库，销售地点以地址 + 经纬度坐标形式展示；数据库与 API 已预留地图 SDK 接入字段。
- 目标 Linux 主机未提供 OS 级部署凭据，未覆盖其现有 8080 服务；本地 JDK 17 部署与 Linux 配置包已完成。
- 原始 SQL 中 6 条药品图片使用历史 `localhost:8080` 绝对地址，且原始图片文件未随仓库提供；Docker 中新上传图片及持久卷链路已验证正常。

**安全说明：**

- 远程 MySQL/Redis 密码仅通过环境变量或 Docker Secrets 注入，不写入仓库与文档。
- 登录密码 BCrypt 存储；Token 为随机不透明令牌，Redis 中仅存 SHA-256 摘要，不存明文。
- 演示管理员账号来自原始 SQL，正式上线前应修改默认密码并创建最小权限数据库账号。

---

*更多入口：顶层 [`README.md`](../README.md) · 部署 [`deploy/README.md`](../deploy/README.md) · 接口测试 [`api-tests/README.md`](../api-tests/README.md) · UML [`uml-diagrams/README.md`](../uml-diagrams/README.md) · CI/CD [`process-docs/CICD工程师/README.md`](../process-docs/CICD工程师/README.md)*
