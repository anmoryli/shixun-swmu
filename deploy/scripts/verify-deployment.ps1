param(
    [string]$BaseUrl = 'http://localhost:8082'
)

$ErrorActionPreference = 'Stop'
$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
if ($health.status -ne 'UP') {
    throw "Health check failed: $($health | ConvertTo-Json -Compress)"
}

$loginStatus = & curl.exe -sS -o NUL -w '%{http_code}' --max-time 10 -X POST -H 'Content-Type: application/x-www-form-urlencoded' --data 'username=invalid&password=invalid' "$BaseUrl/api/login"
Write-Host "health=UP"
Write-Host "login_contract_http=$loginStatus"
