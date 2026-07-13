# 药品示例图导入器

`import-sample-drug-images.ps1` 将项目已有的 6 张 JPEG 示例图上传到后端，并按精确药名更新原始 SQL 中的 6 条药品记录。它不会新增药品，也不会修改业务源码。

## 映射

| 药品精确名称 | 本地图片 |
|---|---|
| 复方感冒灵颗粒(双蚁) | `medicine-assorted.jpg` |
| 连花清瘟胶囊 (以岭) | `medicine-capsule.jpg` |
| 布洛芬混悬液 (美林) | `medicine-bottle.jpg` |
| 复方对乙酰氨基酚片(散利痛) | `medicine-blister.jpg` |
| 复方氨酚烷胺片(感叹号) | `medicine-colorful.jpg` |
| 氨咖黄敏胶囊(禾穗速校) | `medicine-hand.jpg` |

图片目录为 `medical-managerment-system/src/assets/medical-samples/`。脚本会在联网前检查文件存在、大小小于 2 MiB 且具有 JPEG 文件签名。

## 使用方法

先启动后端，并使用管理员账号。推荐通过环境变量传入凭据，避免密码进入命令历史：

```powershell
$env:ADMIN_USERNAME = '<管理员账号>'
$env:ADMIN_PASSWORD = '<管理员密码>'

# 只查看计划：会登录、精确查询和检查现有图片，但不会上传或更新药品。
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  .\medical-managerment-system\scripts\import-sample-drug-images.ps1 -WhatIf

# 正式导入。已有图片能 GET 到 HTTP 200 的药品会自动跳过。
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  .\medical-managerment-system\scripts\import-sample-drug-images.ps1

# 强制替换全部 6 条药品的图片。
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  .\medical-managerment-system\scripts\import-sample-drug-images.ps1 -Force
```

后端不在默认地址时可传入 `-BaseUrl`。脚本接受服务根地址或以 `/api` 结尾的 API 地址：

```powershell
.\medical-managerment-system\scripts\import-sample-drug-images.ps1 `
  -BaseUrl 'http://127.0.0.1:18082'
```

也可以显式传入 `-Username` 和 `-Password`，但不建议在共享终端或会保存历史的环境中直接写明文密码。

## 行为与失败处理

每条记录依次执行：管理员登录、精确药名查询、multipart 图片上传、全字段 `PUT` 更新、图片 `GET` 200 验证、药品字段与销售地点关联复查。脚本会保留原有说明、功效、发布者和去重后的 `saleIds`。

- 默认情况下，当前图片 URL 能返回 HTTP 200 时会跳过该药品；`-Force` 可覆盖。
- `-WhatIf` 不执行上传和 `PUT`，但登录会临时创建会话，脚本结束时会主动退出。
- 任一步失败都会输出明确原因并以退出码 `1` 结束。
- 后端目前没有图片删除接口。若上传成功但随后更新失败，脚本会报告可能的孤儿图片 URL，便于人工清理；不要盲目重复运行。
- 导入成功后再次运行默认命令具有幂等效果，因为可访问的现有图片会被跳过。

完成后可清除当前 PowerShell 进程中的凭据：

```powershell
Remove-Item Env:ADMIN_USERNAME, Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
```
