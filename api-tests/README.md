# 接口测试资产

本目录提供两套彼此独立的黑盒测试入口，均以当前 Spring Boot 控制器和 Vue API 调用契约为基线：

- `postman/medicine-api.postman_collection.json`：人工联调、Postman Collection Runner 和 Newman 使用。
- `run_api_tests.py`：无需第三方包的 Python 3 自动回归脚本，适合本机、CI 和部署验收。

所有账号密码仅从本地环境读取。示例环境中不包含远程 MySQL、Redis 或业务账号密钥。

## Python 回归

PowerShell：

```powershell
$env:BASE_URL = 'http://localhost:8082'
$env:ADMIN_USERNAME = '<管理员账号>'
$env:ADMIN_PASSWORD = '<管理员密码>'
python .\api-tests\run_api_tests.py
```

可选变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `API_TIMEOUT_SECONDS` | `15` | 单请求超时秒数 |
| `API_EVIDENCE_DIR` | `process-docs/evidence/api` | JSON、JUnit XML、Markdown 报告目录 |
| `TEST_CITY_NUMBER` | 自动候选 | 必须是 `sysregion` 中存在、`city` 中尚未配置的编号 |
| `TEST_DOCTOR_PASSWORD` | `ApiTest@123456` | 临时医生的初始密码，仅存在进程内 |

脚本覆盖：健康检查、登录成功/失败、缺参、Token 失效、权限树、仪表盘、8 个业务模块的可用 CRUD、图片上传、重复数据、管理员/医生越权边界、密码重置、退出登录。临时业务记录带唯一 `runId`，并按“药品/政策 -> 医生/销售地点/公司/城市”的依赖逆序删除。上传测试会在服务端留下一个 1x1 PNG；当前 API 没有文件删除接口。

退出码：全部通过或仅有显式跳过时为 `0`，存在失败时为 `1`。即使服务不可用或登录失败，脚本也会生成报告。

## Postman / Newman

1. 导入 collection 和 `medicine-local.postman_environment.example.json`。
2. 复制环境，填写 `adminUsername`、`adminPassword`，不要把带密钥的环境文件提交到 Git。
3. 确保 `testCityNumber` 是一个尚未加入业务城市表的有效 `sysregion.id`。
4. 在 Postman 桌面端执行整个 collection。图片上传如未自动解析路径，请在请求中重新选择任意小于 2 MB 的 JPG/PNG。

Newman 示例：

```powershell
newman run .\api-tests\postman\medicine-api.postman_collection.json `
  -e .\path\to\medicine-local.private.postman_environment.json `
  --reporters cli,junit,json `
  --reporter-junit-export .\process-docs\evidence\api\newman.xml `
  --reporter-json-export .\process-docs\evidence\api\newman.json
```

Collection 使用动态 `runId`，并从查询响应中保存临时 ID；请按默认顺序整套运行，不要并发执行同一个环境。若中途终止，请用对应查询请求按 `runId` 定位并删除临时数据。

## 报告与保密

Python 报告只记录目标 URL、状态码、响应体和断言，不记录请求头、密码或 Token。`process-docs/evidence/api/` 用于保存每次联调的证据，建议把带时间戳的报告纳入过程文档；失败响应如包含业务敏感数据，应在对外分发前人工脱敏。
