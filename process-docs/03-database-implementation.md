# 数据库实施与验证记录

## 1. 目标环境

| 项目 | 实际值 |
|---|---|
| MySQL 主机 | `106.54.210.109:3306` |
| MySQL 版本 | `8.0.46-0ubuntu0.24.04.2` |
| 目标 schema | `medicine` |
| schema 字符集 | `utf8mb4` |
| schema 排序规则 | `utf8mb4_unicode_ci` |
| Redis 主机 | `106.54.210.109:6379` |
| Redis 版本 | `7.0.15` |

密码只在运行环境中使用，不写入 Git、配置样例或本文档。

## 2. 原始 SQL 审计

- 文件：`sql/medical.sql`
- 大小：911,732 bytes
- SHA-256：`DAB732E30BD974B4E273C127BC1C96BC0320B2328BF6F40E0BE9845A387D2DFA`
- 表：17 张
- 单行 INSERT：7,446 条
- 历史来源库名：`bin_text`，文件本身没有 `CREATE DATABASE` 或 `USE`
- 风险：包含 17 个 `DROP TABLE IF EXISTS`；关闭外键检查后没有在脚本末尾恢复。

执行前远程实例仅有系统库和 `zhixue`，不存在 `medicine`，因此创建目标库不会覆盖已有 schema。

## 3. 原始 SQL 导入

执行流程：

1. 显式创建 `medicine`，字符集设为 `utf8mb4`。
2. 新连接明确选择 `medicine`。
3. 使用 UTF-8 客户端执行原始 `medical.sql`。
4. 连接结束后重新连接，通过 `information_schema` 和逐表计数验证。

执行结果：

| 指标 | 结果 |
|---|---:|
| 执行耗时 | 40.4 秒 |
| SQL 结果段 | 7,481 |
| 成功创建表 | 17 |
| 导入异常 | 0 |

初始表与行数：

| 表 | 行数 |
|---|---:|
| account | 16 |
| china | 3,555 |
| city | 11 |
| company_policy | 4 |
| doctor | 12 |
| doctor_level | 3 |
| drug | 6 |
| drug_sale | 22 |
| drugcompany | 4 |
| material | 7 |
| medical_policy | 2 |
| patient | 0 |
| permission | 11 |
| role_permission | 18 |
| sale | 18 |
| sysregion | 3,750 |
| treat_type | 7 |

## 4. 需求对齐迁移

迁移文件：`sql/migrations/V2__requirements_alignment.sql`。

迁移先在临时 schema `medicine_migration_test` 完整执行原始导入和迁移，验证通过后删除临时 schema，再应用于正式 `medicine`。

测试结果：

```text
status=PASS
tables=20
role2_permissions=9
quality_issues=12
drug_sale_after_cleanup=13
sale_longitude_column=true
```

正式迁移耗时 2.05 秒，完成以下变更：

- `account` 增加账号状态和最后登录时间。
- `sale` 增加地址、经度、纬度及范围检查。
- 医生角色补齐公司、销售地点和城市三个只读菜单。
- 移除指向缺失 404 前端组件的角色菜单分配。
- 新建 `news`，为首页资讯和仪表盘提供数据源。
- 新建 `password_reset_audit`，保留医生密码重置审计。
- 新建 `data_quality_issue`，保存原始 SQL 的脏数据证据。
- 清理 9 条同时引用不存在药品/药店的 `drug_sale` 关系后补充外键。
- 补充核心唯一索引、查询索引和关系外键。

迁移后验证：

| 指标 | 结果 |
|---|---|
| 表总数 | 20 |
| 医生权限 | `1,2,3,4,5,6,7,8,10` |
| 首页资讯 | 3 条 |
| 有效药品—药店关系 | 13 条 |
| 数据质量记录 | 12 条 |

数据质量记录分类：

- `ORPHAN_DRUG_SALE`：9 条，已归档并清理。
- `INVALID_CITY_REGION`：1 条，保留业务记录并在接口中容错显示。
- `INVALID_PHONE`：1 条，保留原始数据供后续人工确认。
- `REGION_SELF_LOOP`：1 条，保留并要求树查询显式防循环。

## 5. Redis 验证

- 认证连接成功，`PING=true`。
- 实施前 `DBSIZE=0`。
- 后端只保存不可逆摘要后的登录 Token key，并设置过期时间；不在 Redis 保存明文密码。

## 6. 回滚和重建

原始 SQL 和 V2 迁移分开管理：

1. 重建时新建空 `medicine`。
2. 执行 `sql/medical.sql`。
3. 执行 `sql/migrations/V2__requirements_alignment.sql`。
4. 按本文档中的表数、权限和质量记录进行校验。

不要在已有业务数据的 schema 上重复执行原始 SQL；其中的 `DROP TABLE` 会删除现有表。
