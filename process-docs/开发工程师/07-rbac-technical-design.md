# RBAC 权限模型技术方案与实施说明

## 1. 目标与边界

本次改造把原来的“账号字符串角色 + 菜单表 + 控制器硬编码角色”升级为可扩展的 RBAC（Role-Based Access Control）模型。授权结论只由服务端产生，前端菜单和按钮隐藏仅用于降低误操作，不作为安全边界。

改造遵循以下约束：

- 不删除 `account.utype`、`permission` 和 `role_permission`，保证旧版本可以回滚；
- 新关系使用数值外键和稳定编码，支持一个账号拥有多个角色；
- 菜单权限（`MENU`）与接口动作权限（`ACTION`）共用统一权限目录，但查询时严格按类型过滤；
- 每个请求重新读取账号状态、角色和动作权限，角色撤销或账号禁用无需等待 Redis 会话自然过期；
- 默认拒绝：没有有效账号、启用角色或动作权限时，不能访问受保护业务接口。

本项目当前提供管理员和医生两类可用角色，患者角色仅作为禁用的扩展占位，不授予业务能力。

## 2. 数据模型

### 2.1 关系与职责

| 表 | 关键字段 | 职责 |
|---|---|---|
| `rbac_role` | `id`、`code`、`enabled` | 角色主数据；`ADMIN`、`DOCTOR` 为当前启用角色，`PATIENT` 默认禁用 |
| `account_role` | `account_id`、`role_id`、`is_primary` | 账号与角色多对多关系；主角色标记用于兼容旧 `utype` |
| `permission` | `code`、`permission_type`、`enabled`、`sort_order` | 统一权限目录；菜单记录保留路径/组件，动作记录使用稳定权限码 |
| `rbac_role_permission` | `role_id`、`permission_id` | 角色到菜单/API 权限的多对多授权关系 |
| `account`（兼容） | `utype`、`status` | 保留旧登录和回滚字段；运行时只接受启用账号 |
| `role_permission`（兼容） | `roleName`、`per_id` | V5 回填来源和旧版本回滚数据，不再作为新授权查询入口 |

`account_role.primary_account_id` 是由 `is_primary` 计算的唯一列，同一账号最多只能有一个主角色；普通角色可继续通过复合主键追加。V5 通过外键、唯一索引和检查约束阻止悬空关系、重复权限码和非法启停值。

### 2.2 权限码约定

动作权限统一采用 `resource:verb` 形式，菜单采用 `menu:<name>` 形式。控制器注解、Redis 外的当前授权快照以及前端能力判断都使用同一字符串，避免“菜单可见但 API 未保护”的漂移。

| 资源 | 查询 | 写入/管理 | 备注 |
|---|---|---|---|
| 首页 | `dashboard:read` | - | 仪表盘统计和资讯 |
| 公司 | `company:read` | `company:write` | 医药公司 CRUD |
| 销售地点 | `sale:read` | `sale:write` | 列表和坐标 |
| 销售地图 | `sale-map:read` | - | 地图与逆地理编码 |
| 城市 | `city:read` | `city:write` | 城市维护 |
| 药品 | `drug:read` | `drug:write` | 药品 CRUD |
| 医保政策 | `medical-policy:read` | `medical-policy:write` | 政策 CRUD |
| 公司政策 | `company-policy:read` | `company-policy:write` | 政策 CRUD |
| 医生 | `doctor:read` | `doctor:write`、`doctor:reset-password` | 医生档案和账号 |
| 材料 | `material:read` | `material:write` | 必备材料 CRUD |
| 文件 | - | `file:upload` | 药品图片上传 |

### 2.3 角色授权矩阵

| 能力 | `ADMIN` | `DOCTOR` |
|---|:---:|:---:|
| 所有 `MENU` 权限 | ✓ | 按业务菜单种子 |
| 所有 `ACTION` 权限 | ✓ | - |
| 上述资源 `:read` | ✓ | ✓（含 `doctor:read`，但不自动显示医生管理菜单） |
| 上述资源 `:write`、重置密码、上传 | ✓ | - |

医生保留既有只读接口兼容行为，但没有写权限；是否在前端展示某个菜单不能改变后端注解的最终判断。

## 3. 运行时授权链路

1. 登录接口按用户名查询 `account`，校验 BCrypt 密码和 `status=1`，创建不透明随机 Token。
2. Redis 只保存 Token 的 SHA-256 摘要键和最小会话身份；原始 Token 仅通过 httpOnly Cookie 交付浏览器。
3. `TokenAuthenticationFilter` 取 Cookie（兼容期也接受 Bearer 头），读取会话中的 `userId`，再按账号 ID 查询启用角色和启用权限。
4. 过滤器把 `ROLE_<role.code>` 与动作权限码写入 Spring Security `GrantedAuthority`。账号不存在、已禁用或没有启用角色时不建立认证上下文；有启用角色但没有某项动作权限时仍可保持登录，但该动作由控制器按默认拒绝处理。
5. 控制器使用 `@PreAuthorize("hasAuthority('resource:verb')")` 做接口级边界；`/api/dashboard`、`/api/regeo` 等原先只有 `authenticated` 的接口也具有明确动作权限。
6. `/api/permissions` 只使用认证主体的 `userId` 查询多角色并集，返回 `permissions` 菜单树、`permissionCodes` 和 `roles`。客户端传入的 `roleName` 不参与授权。
7. 前端 Vuex 保存能力码和已授权路由，`$can()` 只控制交互显示；退出、会话过期和账号切换时清空能力并移除动态路由。

由于权限查询不依赖 Redis 中冻结的角色字符串，角色权限撤销和账号禁用在下一次请求即可生效；Redis TTL 仍只负责会话生命周期和滑动续期。

## 4. 迁移、发布与回滚

执行顺序固定为：

```text
sql/medical.sql
sql/migrations/V2__requirements_alignment.sql
sql/migrations/V3__password_audit_delete_policy.sql
sql/migrations/V4__sale_map_menu.sql
sql/migrations/V5__normalized_rbac.sql
```

V5 是加法迁移，具备以下保护：

- 角色、关系表使用 `CREATE TABLE IF NOT EXISTS`；列、索引和检查约束通过幂等辅助过程增加；
- 既有菜单先生成稳定权限码，再检查空值/重复值，之后才建立唯一索引；
- 未知 `account.utype` 直接 `SIGNAL` 中止，`ROLE_3` 映射到默认禁用的 `PATIENT`，不会静默提权；
- 历史菜单只复制 `permission_type=MENU`，动作权限由显式种子建立；
- 迁移不删除旧表，回滚时可以重建空库并只执行基线和 V2-V4。

生产执行前必须备份 `medicine`，在隔离 schema 先完整跑一遍并核验：角色数量、权限码唯一性、账号主角色唯一性、外键、管理员全量权限和医生无写权限。V4 不能在 V5 之后执行；如果新增菜单，必须同时补权限码和角色授权。

## 5. 测试与验收门禁

本次实现的自动化回归重点包括：

- 后端登录角色格式、会话主体、账号状态字段、医生角色绑定和原有业务服务回归；
- `/api/permissions` 按账号 ID 查询并忽略客户端角色参数的控制器契约；
- 前端权限码/角色归一化、显式空权限默认拒绝、路由白名单、动态路由卸载和 API 无 `roleName` 参数；
- 9 个业务页面的具体写权限显示与方法级 fail-closed 防护；
- 前端全量 129 项 Vitest 回归与 Vite 生产构建。

仓库既有准出命令仍为后端 `mvn -B -ntp clean verify`、前端 `npm run test:coverage` 和 `npm run build`。本地已验证后端 93 项测试、前端 129 项测试以及生产构建通过；覆盖率阈值由 CI 按仓库配置继续执行。黑盒 API 回归必须使用 Cookie 会话，不从登录响应体读取已经不再暴露的 Token。

## 6. 威胁与控制

| 风险 | 控制 |
|---|---|
| 客户端伪造角色参数 | `/api/permissions` 忽略 `roleName`，按认证主体账号 ID 查询 |
| 只隐藏前端按钮导致越权 | 每个业务方法保留服务端 `@PreAuthorize` |
| 禁用账号仍持有旧会话 | 每请求联查 `account.status`，不建立认证上下文 |
| 权限菜单/API 漂移 | MENU/ACTION 统一权限码目录，迁移时唯一约束和类型过滤 |
| SQL 注入 | MyBatis 参数绑定；权限码来自受控迁移种子，不拼接用户输入 |
| 会话泄露 | Redis 存 SHA-256 摘要，Cookie httpOnly，生产启用 Secure/HTTPS |
| 迁移误操作 | 版本化、幂等、备份、隔离 schema 预演和旧表保留 |
