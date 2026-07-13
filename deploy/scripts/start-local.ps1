param(
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$jar = Join-Path $root 'medical-backend\target\medical-backend-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw "Backend JAR not found: $jar. Run mvn clean package first."
}

$required = 'DB_PASSWORD', 'REDIS_PASSWORD'
foreach ($name in $required) {
    if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
        throw "Required environment variable is missing: $name"
    }
}

$env:SERVER_PORT = $Port
$env:DB_URL = if ($env:DB_URL) { $env:DB_URL } else { 'jdbc:mysql://106.54.210.109:3306/medicine?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' }
$env:DB_USERNAME = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { 'root' }
$env:REDIS_HOST = if ($env:REDIS_HOST) { $env:REDIS_HOST } else { '106.54.210.109' }
$env:REDIS_PORT = if ($env:REDIS_PORT) { $env:REDIS_PORT } else { '6379' }

& java -jar $jar

