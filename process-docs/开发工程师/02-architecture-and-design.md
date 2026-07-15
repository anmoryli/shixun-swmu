# 概要设计与详细设计

## 1. 总体架构

```mermaid
flowchart LR
    U[管理员或医生] --> N[Nginx/开发服务器]
    N --> V[Vue 3 前端]
    N --> B[Spring Boot API]
    B --> S[Spring Security]
    S --> R[(Redis 7 登录会话)]
    B --> M[MyBatis 数据访问]
    M --> D[(MySQL 8 medicine)]
    B --> F[安全图片存储]
```

采用前后端分离架构。生产环境由 Nginx 同源发布前端，并代理 `/api` 到后端，避免浏览器跨域和 `localhost` 硬编码问题。

## 2. 后端分层

```text
controller  HTTP 契约、参数校验、角色注解
    ↓
service     业务规则、事务、引用校验、分页组装
    ↓
mapper      MyBatis 参数化 SQL
    ↓
MySQL       medicine schema
```

公共横切能力：

- `common`：统一响应、分页、业务异常和全局异常处理。
- `security`：Token 生成/摘要、Redis 会话、按账号实时装载 authorities、请求过滤和 401/403 处理。
- `config`：Spring Security、CORS、MyBatis 与上传配置。
- `auth`：登录、退出、规范化 RBAC 查询和动态权限菜单。
- `business`：八类业务模块、仪表盘与上传。

## 3. 登录与授权时序

```mermaid
sequenceDiagram
    participant C as Vue客户端
    participant A as 登录接口
    participant F as TokenAuthenticationFilter
    participant DB as MySQL
    participant R as Redis
    C->>A: username/password
    A->>DB: 按用户名查询账号
    A->>DB: 确认账号至少绑定一个启用角色
    A->>A: BCrypt校验密码和status=1
    A->>R: 保存Token摘要和登录态(TTL)
    A-->>C: httpOnly Cookie 下发令牌 + userInfo(utype=1/2)
    C->>F: Cookie自动携带 + GET /permissions
    F->>R: 用Token摘要恢复AuthSession(userId)
    F->>DB: 查询启用账号、角色和ACTION权限
    F->>F: 构造ROLE_*和resource:verb authorities
    F->>A: 已认证请求
    A->>DB: 按userId查询MENU树、ACTION权限码、角色
    A-->>C: permissions + permissionCodes + roles
```

安全原则：

- 不相信客户端提交的 `roleName` 或前端缓存的角色。
- Redis key 使用 Token 的 SHA-256，不把原始 Token 作为 key。
- 每个请求重新查询账号状态、启用角色和动作权限；账号禁用或授权撤销在下一请求生效。
- 控制器使用 `@PreAuthorize("hasAuthority('resource:verb')")`；前端菜单、按钮和 `$can()` 不是安全边界。
- 没有有效账号、启用角色或目标动作权限时默认拒绝；医生只有明确授予的业务读取权限。
- Token 过期或退出后返回前端可识别的 `10006`。

### 3.1 运行时 RBAC 请求链

```mermaid
flowchart LR
    Q[Cookie 或 Authorization] --> T[TokenAuthenticationFilter]
    T --> R[(Redis Token摘要会话)]
    R --> U[userId]
    U --> D[(MySQL account/account_role/rbac_role)]
    D --> P[(rbac_role_permission/permission ACTION)]
    P --> G[Spring GrantedAuthority]
    G --> A{"@PreAuthorize<br/>hasAuthority"}
    A -->|允许| C[Controller/Service]
    A -->|拒绝| X[403 / code 10003]
```

过滤器只从 Redis 取最小会话身份，不把登录瞬间的角色当作整个 TTL 内不变的授权快照。`PermissionMapper` 以 `userId` 联查 `account.status=1`、启用角色和启用 `ACTION` 权限；角色 authority 使用 `ROLE_<rbac_role.code>`，动作 authority 保持原始 `resource:verb`。`/api/permissions` 再查询 `MENU` 权限并返回动作权限码与角色并集，客户端参数不参与决策。

前端在 Vuex 内存中保存 `permissionCodes`、`roles` 和授权路由路径，通过 `$can('drug:write')` 等能力码控制按钮；路由守卫只允许服务端菜单树中的路径。退出、会话失效或账号切换会卸载动态路由并清空权限。即使前端检查被绕过，后端方法级权限仍会拒绝未授权请求。

## 4. 数据模型

```mermaid
erDiagram
    ACCOUNT ||--o| DOCTOR : owns
    DOCTOR_LEVEL ||--o{ DOCTOR : classifies
    TREAT_TYPE ||--o{ DOCTOR : treats
    DRUGCOMPANY ||--o{ COMPANY_POLICY : publishes
    CITY ||--o{ MEDICAL_POLICY : applies_to
    DRUG ||--o{ DRUG_SALE : sold_at
    SALE ||--o{ DRUG_SALE : sells
    ACCOUNT ||--o{ ACCOUNT_ROLE : assigned
    RBAC_ROLE ||--o{ ACCOUNT_ROLE : contains
    RBAC_ROLE ||--o{ RBAC_ROLE_PERMISSION : grants
    PERMISSION ||--o{ RBAC_ROLE_PERMISSION : authorized
    PERMISSION ||--o{ ROLE_PERMISSION : legacy_menu
    ACCOUNT ||--o{ PASSWORD_RESET_AUDIT : target
```

V5 后 `permission` 是统一目录：带路径/组件的记录属于 `MENU`，后端动作属于 `ACTION`；稳定 `code` 是前后端共同使用的授权标识。`account_role` 和 `rbac_role_permission` 由外键、复合主键和唯一约束保护。旧 `account.utype`、`role_permission` 仍保留用于回填和旧版本回滚，但新运行时不再按文本角色名查询授权。原始脏关系先归档到 `data_quality_issue` 再清理，过程见数据库实施记录。

## 5. 事务边界

- 医生新增：账号唯一性检查、账号插入、医生插入在同一事务。
- 医生删除：先校验引用，再删除医生和账号；失败整体回滚。
- 密码重置：更新 BCrypt 密码、失效旧 Token、写审计在同一事务。
- 药品新增/修改：药品与 `drug_sale` 关系在同一事务。
- 公司/城市删除：存在政策引用时返回冲突，不级联误删。

## 6. 上传设计

1. 最大 2 MB。
2. 扩展名与 Content-Type 仅允许 JPEG/PNG。
3. 使用图片解码进一步校验真实文件内容。
4. 服务端生成 UUID 文件名，拒绝用户路径。
5. 上传目录通过 `app.upload.directory` 配置，生产推荐 `/opt/medicine/uploads`。
6. 返回 URL 通过公开基址配置生成，不写死 localhost。

## 7. 配置与凭据

- 仓库仅提交环境变量占位和 `.example` 文件。
- 本地测试通过进程环境变量注入数据库和 Redis 密码。
- 服务器使用权限为 `0600` 的 `/etc/medicine/medicine-backend.env`。
- 正式环境建议创建 `medicine_app` 最小权限账号；本次按用户提供账号完成远程联调。
