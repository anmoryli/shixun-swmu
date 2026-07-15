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

## 4. 增量迁移（V2—V5）

### 4.1 V2/V3：需求对齐和审计删除策略

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

接口回归发现“重置密码后删除医生”会被审计外键阻止，因此新增
`sql/migrations/V3__password_audit_delete_policy.sql`，把密码重置审计设为账号生命周期级联。
V3 已在远程 `medicine` 执行并验证 `DELETE_RULE=CASCADE`；修复后删除医生、账号和对应测试审计可在同一事务完成。

V2/V3 已执行验证：

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

### 4.2 V4：销售地图菜单

`sql/migrations/V4__sale_map_menu.sql` 新增 `BaseSaleMap` 菜单，并分别授权管理员和医生角色。V4 不新增数据表，但它必须先于 V5 执行：V5 会为当时已经存在的菜单生成稳定 `menu:*` 权限码，并把旧 `role_permission` 菜单授权回填到规范化关系。

### 4.3 V5：规范化 RBAC

迁移文件：`sql/migrations/V5__normalized_rbac.sql`。固定执行顺序为：

```text
sql/medical.sql
sql/migrations/V2__requirements_alignment.sql
sql/migrations/V3__password_audit_delete_policy.sql
sql/migrations/V4__sale_map_menu.sql
sql/migrations/V5__normalized_rbac.sql
```

V5 使用 MySQL CLI 的 `DELIMITER` 和辅助存储过程，不能在只支持逐条语句的 SQL 面板中拆开执行。它是加法、幂等迁移，主要变更如下：

- 新建 `rbac_role`：稳定角色主数据，内置启用的 `ADMIN`、`DOCTOR` 和默认禁用的 `PATIENT`。
- 新建 `account_role`：账号—角色多对多关系，通过外键、复合主键和生成列唯一索引保证同一账号最多一个主角色。
- 新建 `rbac_role_permission`：角色—权限多对多关系，取代文本角色名关联。
- 为 `permission` 增加 `code`、`permission_type`、`description`、`enabled`、`sort_order`；`code` 唯一且非空，类型只能为 `MENU` 或 `ACTION`。
- 既有菜单转换为 `menu:*` 权限码；无法识别的扩展菜单使用 `menu:legacy:<id>`，避免静默丢失。
- 新增 dashboard、公司、销售地点、地图、城市、药品、两类政策、医生、材料和文件上传的 `resource:read`/`resource:write` 动作权限。
- 从 `account.utype` 回填 `account_role`，从旧 `role_permission` 回填菜单授权；管理员获得全部启用权限，医生只获得明确列出的只读动作。
- 保留 `account.utype` 和 `role_permission`，供旧版本回滚读取；新版本运行时按账号 ID 查询三张规范化关系表。

迁移前置校验会在存在未知 `account.utype` 时以 `SQLSTATE 45000` 中止；权限码补齐后还会检查空值和重复值，再建立唯一索引。V5 文件末尾删除临时存储过程并恢复会话 `sql_mode`。

### 4.4 V5 后目标状态和校验

V2 新增 3 张表，V5 再新增 3 张表，因此从 17 张基线表完整迁移后的目标总数为 **23 张**。以下是执行 V5 后的必做校验；其中“23 张”适用于未自行扩展表的仓库标准基线：

```sql
-- 1. 基线表数
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema='medicine' AND table_type='BASE TABLE';

-- 2. 角色启停状态
SELECT code, enabled FROM rbac_role ORDER BY code;

-- 3. 权限码必须非空且唯一（预期空集）
SELECT code, COUNT(*) AS duplicates
FROM permission
GROUP BY code
HAVING code IS NULL OR code='' OR COUNT(*) > 1;

-- 4. 每个历史账号都应完成主角色回填（预期 0）
SELECT COUNT(*) AS accounts_without_primary_role
FROM account a
LEFT JOIN account_role ar
  ON ar.account_id=a.id AND ar.is_primary=1
WHERE ar.account_id IS NULL;

-- 5. 同一账号不能有多个主角色（预期空集）
SELECT account_id, SUM(is_primary=1) AS primary_count
FROM account_role
GROUP BY account_id
HAVING primary_count <> 1;

-- 6. 医生不能拥有写入、重置密码或上传权限（预期 0）
SELECT COUNT(*) AS doctor_forbidden_actions
FROM rbac_role_permission rrp
JOIN rbac_role r ON r.id=rrp.role_id
JOIN permission p ON p.id=rrp.permission_id
WHERE r.code='DOCTOR'
  AND p.permission_type='ACTION'
  AND (p.code LIKE '%:write'
       OR p.code IN ('doctor:reset-password', 'file:upload'));

-- 7. 管理员应覆盖全部启用权限（预期 0）
SELECT COUNT(*) AS admin_missing_permissions
FROM permission p
WHERE p.enabled=1
  AND NOT EXISTS (
    SELECT 1
    FROM rbac_role_permission rrp
    JOIN rbac_role r ON r.id=rrp.role_id
    WHERE rrp.permission_id=p.id AND r.code='ADMIN' AND r.enabled=1
  );
```

标准结果应为：`table_count=23`；`ADMIN`、`DOCTOR` 启用，`PATIENT` 禁用；其余异常查询为 `0` 或空集。还应通过 `information_schema.referential_constraints` 检查 `account_role`、`rbac_role_permission` 外键，并完成管理员写入成功、医生读取成功、医生写入返回 403 的应用级回归。本文档没有当前远程实例已执行 V5 的实时证据，因此 23 表及上述结果是迁移后的验收目标，不能替代目标环境实测记录。

## 5. Redis 验证

- 认证连接成功，`PING=true`。
- 实施前 `DBSIZE=0`。
- 后端只保存不可逆摘要后的登录 Token key 和最小会话身份，并设置过期时间；不在 Redis 保存明文密码。
- V5 后角色和动作权限不作为整个 TTL 内冻结的 Redis 快照；过滤器按会话 `userId` 在每个请求重新查询启用账号、角色和权限。

## 6. 回滚、失败恢复和重建

### 6.1 执行前保护

生产或共享环境执行 V5 前必须：

1. 使用与 MySQL 服务器版本匹配的 `mysqldump --single-transaction --routines --triggers` 生成完整备份，并记录应用镜像/JAR 版本。
2. 在隔离 schema 从 `medical.sql` 开始完整执行 V2—V5，完成第 4.4 节校验。
3. 停止写流量或安排维护窗口；MySQL DDL 会隐式提交，不能把整份 V5 当作单事务回滚。
4. 检查 `account.utype` 仅包含 `ROLE_1`、`ROLE_2`、`ROLE_3`，并提前处理自定义菜单权限码冲突。

### 6.2 V5 失败或应用回滚

V5 的表、列、索引和数据写入均按幂等方式设计。若因未知角色或重复权限码中止，应先保留错误日志和数据库快照，修复明确的数据问题，再从独立 MySQL CLI 连接重新执行完整 V5；不要跳过中间语句或手工伪造授权。

旧 `account.utype` 和 `role_permission` 未删除，因此短期应用回滚到 V4 版本时可以保留 V5 新表/新列，旧应用仍能读取兼容数据。若必须把数据库结构也恢复到 V4，优先停止流量并恢复 V5 前完整备份；不建议在线手工删除新外键、索引、列和表，因为 DDL 不可事务回滚且容易留下半回滚状态。应用镜像/JAR 回滚本身不会撤销 SQL。

### 6.3 从零重建

1. 新建空 `medicine`，不要覆盖已有业务 schema。
2. 执行 `sql/medical.sql`。
3. 执行 `sql/migrations/V2__requirements_alignment.sql`。
4. 执行 `sql/migrations/V3__password_audit_delete_policy.sql`。
5. 执行 `sql/migrations/V4__sale_map_menu.sql`。
6. 执行 `sql/migrations/V5__normalized_rbac.sql`。
7. 按本文档中的 23 表、角色、权限、外键和质量记录进行校验。

若重建的是旧 V4 应用的专用回滚环境，只执行到第 5 步；不要在已有业务数据的 schema 上重复执行原始 `medical.sql`，其中的 `DROP TABLE` 会删除现有表。
