# 慧医数字医疗应用系统 100% 行覆盖测试报告（2026-07-15）

## 1. 测试结论

本轮在 `codex/100-percent-test-coverage` 分支补齐前后端测试，并将“全量源码行覆盖率 100%”固化为构建硬门禁。最终可执行自动化测试共 **215 项，215 项通过，0 项失败，0 项跳过**；后端 `mvn verify`、前端覆盖率门禁、前端生产构建、Python 工具自测与 Postman JSON 解析均通过。

| 测试层 | 结果 |
|---|---:|
| 后端 JUnit 5 / Mockito | 84/84 通过 |
| 前端 Vitest / Vue Test Utils | 123/123 通过 |
| API 回归工具 Python unittest | 8/8 通过 |
| 合计 | 215/215 通过 |
| 后端生产构建 | `mvn verify` 通过 |
| 前端生产构建 | `npm run build` 通过 |

结论：**代码级自动化测试、全量源码 100% 行覆盖和生产构建满足本轮准出条件。** 当前执行机仍未提供 MySQL、Redis、Docker 和隔离测试账号，因此没有把 2026-07-13 的部署级 57/57 历史结果冒充本轮实时 API 回归。

## 2. 覆盖率结果

### 2.1 后端 JaCoCo

统计口径为 `medical-backend/src/main/java` 全量生产类，无类、包或源码排除规则。

| 指标 | 覆盖结果 |
|---|---:|
| 行 | **100%（935/935）** |
| 方法 | 100%（326/326） |
| 指令 | 99.74%（3897/3907） |
| 分支 | 87.39%（194/222） |

`pom.xml` 已接入 JaCoCo 0.8.12；`mvn verify` 在 bundle 行覆盖率低于 1.00 时直接失败。

### 2.2 前端 V8

统计口径为 `medical-managerment-system/src` 的全量 JavaScript 与 Vue 单文件组件，没有通过缩小 include 范围抬高结果。

| 指标 | 覆盖结果 |
|---|---:|
| 语句 | **100%** |
| 行 | **100%** |
| 分支 | 95.64% |
| 函数 | 63.88% |

`vite.config.js` 已设置 statements/lines 100% 阈值；任一指标低于 100%，`npm run test:coverage` 退出失败。函数指标受 Vue SFC 编译生成的渲染闭包计数影响，本报告单独披露，不将“100% 行覆盖”表述为“所有覆盖指标均为 100%”。

## 3. 新增测试范围

### 3.1 后端

- 全部业务服务：城市、公司、公司政策、材料、医保政策、销售地点和药品的分页、转换、增删改及关联关系。
- 全部业务控制器：正常响应、重复数据、成功/失败更新、上传 URL、地图和仪表盘委托。
- 安全链路：Cookie 与 Authorization 取 token、已有认证短路、Redis 会话缺失、摘要算法异常、认证失败与越权 JSON 响应。
- 配置：完整 SecurityFilterChain、CORS、密码编码器和上传资源映射。
- DTO/模型：构造器、getter/setter、空列表归一化。
- 全局异常：字段校验有/无 FieldError、绑定异常、业务异常、非法请求、越权和未知异常脱敏。
- 应用入口：验证 `MedicalBackendApplication` 委托 `SpringApplication.run`。

### 3.2 前端

- 全部 Vue 页面、布局和公共组件的实际挂载与渲染。
- 页面方法、计算属性、表单校验、分页两条路径、登录成功/失败/重复提交。
- 图片上传计时与成功回填，首页图表生命周期、时间格式化、降级加载。
- 高德地图加载、选点、反向地理编码、标记刷新、成功与异常路径。
- 所有 Vuex CRUD 模块、根 getter 和 store 自动装配。
- 应用入口、路由守卫、动态菜单解析、Axios 请求/响应拦截器。

### 3.3 API 测试资产

- `api-tests` 8 项工具自测通过。
- `run_api_tests.py` 与 `generate_postman_collection.py` 编译通过。
- 3 个 Postman collection/environment JSON 使用 Python 标准库实际解析，3/3 有效。

## 4. 测试发现与修复

| 编号 | 发现 | 修复 | 复验 |
|---|---|---|---|
| TEST-20260715-02 | Vuex eager glob 会把 `modules/app.test.js` 当作生产模块装载，完整 store 初始化失败 | glob 明确排除 `*.test.js` | store 装配测试、123 项前端测试及生产构建通过 |
| TEST-20260715-03 | 文件目标路径校验和 SHA-256 不可用异常无法通过公开行为稳定注入 | 提取包内可覆盖的 `resolveTarget` 和 `messageDigest`，业务行为不变 | 路径穿越与算法不可用场景通过 |
| TEST-20260715-04 | Spring Boot 2.7 自带旧 Byte Buddy 在 JDK 21 inline mock 下默认拒绝 class version 65 | Surefire 固化 `net.bytebuddy.experimental=true` | 84 项后端测试与 JaCoCo 门禁通过 |

## 5. 最终执行记录

执行环境：Windows PowerShell、Microsoft OpenJDK 21.0.7（`release=17`）、Maven 3.9.9、Node.js 24.16.0、npm 11.13.0、Python 3.12.4。

```powershell
# 后端：84 项测试、打包、JaCoCo 报告及 100% 行覆盖门禁
mvn verify

# 前端：123 项测试、100% statements/lines 门禁、生产构建
npm run test:coverage
npm run build

# API 工具：8 项自测、编译和 3 个 JSON 解析
python -m unittest discover -s api-tests -p test_*.py -v
python -m py_compile api-tests/run_api_tests.py api-tests/generate_postman_collection.py
```

最终执行时间：2026-07-15 10:37–10:39（Asia/Shanghai）。全部命令退出码为 0。测试日志中的地图加载失败、离线异常和全局未知异常堆栈均为测试主动注入的失败场景，不是用例失败。

## 6. 已知限制

- 本轮没有可用的 MySQL、Redis、Docker 和隔离管理员账号，未执行会真实创建并清理数据的部署级 API 回归；历史证据仍位于 `process-docs/evidence/api/`。
- 前端构建保留既有非阻断警告：第三方/旧 CSS 的 IE 星号 hack、部分 chunk 超过 500 kB；构建成功但后续仍建议清理与分包。
- 本轮硬门禁针对全量源码行与语句覆盖；分支、指令和 Vue 编译函数指标已如实单列，不能据此替代真实浏览器兼容性、视觉回归和部署验收。

