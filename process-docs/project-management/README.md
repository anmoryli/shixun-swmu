# 项目管理与功能点清单

## 统计口径

- Story：13 个用户价值项，见 `story-backlog.csv`。
- Task：57 个黑盒验收任务，见 `task-backlog.csv`；另有 Docker/CI/CD 等技术任务可挂在 ST-13 下。
- Epic：5 个，见 `epic-backlog.csv`；Feature：7 个，见 `feature-backlog.csv`；里程碑：5 个，见 `milestones.csv`。
- 后端接口端点：43 个映射；黑盒验收场景：57 个，二者统计口径不同。
- 计划周期：2026-07-13 至 2026-07-17，共 5 个工作日。
- 团队：`anmory666`（项目经理/开发人员）负责认证与后端基础；`Ning_24`（CI/CD 工程师）负责仪表盘、基础资料和流水线；`khyxone`（开发人员）负责医生/药品；`spikezzw`（开发人员）负责材料/政策/上传；`mengyetianxing`（测试人员）负责前端联调、测试交付。

## 工作项字段

文件已按 Epic → Feature → Story → Task 分层；里程碑单独记录阶段验收点。Task 的“父工作项”使用 ST-01 至 ST-13。

## 录入华为云 ProjectMan

按 Epic、Feature、Story、Task、里程碑 CSV 分别创建对应工作项，负责人字段已经直接写入真实账号。迭代统一选择“迭代1”。状态初始为“待开发/待开始”，每天按实际进度改为进行中/已完成/阻塞。
