# 华为云 CodeArts 流水线配置

本目录存放仓库侧的 CI/CD 模板,与华为云 CodeArts 流水线双向同步:

| 文件 | 用途 |
| --- | --- |
| `pipeline.yaml` | CodeArts `yaml-pipeline` 模式的流水线定义(后端 maven、前端 npm、可选 docker 镜像) |
| `../../deploy/scripts/ci-build.sh` | 流水线调用的 bash 构建脚本(也可本地跑) |
| `../../deploy/scripts/ci-build.ps1` | 同上,Windows PowerShell 版本,本地调试用 |

## 修复背景

此前流水线 Maven 步骤直接在仓库根目录执行 `mvn package`,后端 `pom.xml`
位于 `medical-backend/` 子目录,因此报 `MissingProjectException: no POM in this directory`,第一个 maven 任务秒挂。

修复要点(均在 `pipeline.yaml` 和 `deploy/scripts/ci-build.sh`):

1. **Maven 工作目录切到 `medical-backend`**:通过 `workdir: medical-backend`
   + 命令里 `cd medical-backend` 双保险,流水线日志里 `pwd` 也能看到。
2. **步骤拆分**:`checkout → backend → frontend → (可选)docker_build` 四段,
   任何一段失败立即中断。
3. **产物校验**:每个阶段都加 `ls -lh target/*.jar` / `test -d dist` 校验。
4. **私有 nexus/registry 加速**:npm 走 `registry.npmmirror.com`;maven 默认
   走 CodeArts 内置 nexus,如需私有仓库在 step 里加 `-s settings.xml`。

## 在 CodeArts 控制台导入

两种方式选一:

1. **可视化新建**(推荐给非 yaml 用户):在控制台点「新建流水线 → 选择
   `master` 分支 → 添加步骤 Build-Maven(选用模板中提供的 `mvn` 步骤,
   **把「执行路径」改成 `medical-backend`**,命令改为
   `mvn -B -e -Dmaven.test.skip=true clean package`) → 添加
   Build-Npm(执行路径 `medical-managerment-system`,
   命令 `npm config set registry https://registry.npmmirror.com && npm ci && npm run build`)。
2. **yaml-pipeline 模式**:在仓库根目录的 `.codearts/pipeline.yaml` 上点
   「创建流水线」(如果 CodeArts 界面支持「按 yaml 创建」),它会自动把
   `.codearts/pipeline.yaml` 内容导入。

## 本地等效构建

不依赖流水线也能跑同一套:

```bash
bash deploy/scripts/ci-build.sh all
# 或仅后端
bash deploy/scripts/ci-build.sh backend
# 或仅前端
bash deploy/scripts/ci-build.sh frontend
```

Windows PowerShell 版本:

```powershell
.\deploy\scripts\ci-build.ps1 all
```
