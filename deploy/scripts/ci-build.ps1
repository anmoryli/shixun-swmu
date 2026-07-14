# =========================================================================
# 慧医数字医疗应用系统 -- 本地 CI 等价入口 (PowerShell 版)
# 仅用于和 deploy/scripts/ci-build.sh 保持文档/使用一致，PowerShell 不
# 进 CodeArts 流水线，CI 走 bash 版本。Windows 本地构建请用：
#     .\deploy\scripts\ci-build.ps1 backend
# =========================================================================
$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Set-Location $repoRoot

function Build-Backend {
    Write-Host "=== 后端构建 (medical-backend) ===" -ForegroundColor Cyan
    Push-Location "$repoRoot\medical-backend"
    Write-Host "current dir: $((Get-Location).Path)"
    if (-not (Test-Path '.\pom.xml')) {
        Write-Error "ERROR: pom.xml 不在 $(Get-Location)"
    }
    $mvnArgs = @('-B', '-e', 'clean', 'package')
    if ($env:SKIP_TESTS -ne 'false') { $mvnArgs = @('-B', '-e', '-Dmaven.test.skip=true', 'clean', 'package') }
    & mvn @mvnArgs
    if ($LASTEXITCODE -ne 0) { throw "mvn build failed (exit $LASTEXITCODE)" }
    $jar = Get-ChildItem 'target\medical-backend-*.jar' | Select-Object -First 1
    if (-not $jar) { throw 'ERROR: 后端 jar 未生成' }
    Write-Host "OK backend jar: $($jar.Name)" -ForegroundColor Green
    Pop-Location
}

function Build-Frontend {
    Write-Host "=== 前端构建 (medical-managerment-system) ===" -ForegroundColor Cyan
    Push-Location "$repoRoot\medical-managerment-system"
    Write-Host "current dir: $((Get-Location).Path)"
    $node = Get-Command node -ErrorAction SilentlyContinue
    if (-not $node) { Write-Error 'ERROR: node 未安装' }
    Write-Host ("node: {0}, npm: {1}" -f (& node -v), (& npm -v))
    npm config set registry https://registry.npmmirror.com | Out-Null
    if (Test-Path '.\package-lock.json') {
        & npm ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { & npm install --no-audit --no-fund }
    } else {
        & npm install --no-audit --no-fund
    }
    & npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm build failed (exit $LASTEXITCODE)" }
    if (-not (Test-Path '.\dist')) { throw 'ERROR: dist/ 未生成' }
    Write-Host 'OK frontend dist/' -ForegroundColor Green
    Pop-Location
}

switch ($args[0]) {
    'backend'  { Build-Backend }
    'frontend' { Build-Frontend }
    'all'      { Build-Backend; Build-Frontend }
    default    {
        Write-Host '用法: .\deploy\scripts\ci-build.ps1 <target>  (backend|frontend|all)'
        exit 64
    }
}

Write-Host "=== 构建完成 ===" -ForegroundColor Cyan
