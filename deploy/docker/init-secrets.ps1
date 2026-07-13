param(
    [ValidateSet('Both', 'MySQL', 'Redis')]
    [string]$Target = 'Both',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$privateDirectory = Join-Path $root '.work\private\docker'
$mysqlFile = Join-Path $privateDirectory 'mysql-password.txt'
$redisFile = Join-Path $privateDirectory 'redis-password.txt'

function Read-SecretText([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        if ([string]::IsNullOrWhiteSpace($plain)) {
            throw "$Prompt cannot be empty."
        }
        return $plain
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

New-Item -ItemType Directory -Force -Path $privateDirectory | Out-Null
$utf8NoBom = [Text.UTF8Encoding]::new($false)
$selected = switch ($Target) {
    'MySQL' { @(@{ Name = 'MySQL'; File = $mysqlFile }) }
    'Redis' { @(@{ Name = 'Redis'; File = $redisFile }) }
    default {
        @(
            @{ Name = 'MySQL'; File = $mysqlFile },
            @{ Name = 'Redis'; File = $redisFile }
        )
    }
}

foreach ($item in $selected) {
    if (-not $Force -and (Test-Path -LiteralPath $item.File)) {
        throw "$($item.Name) Docker secret already exists. Use -Force only after changing the remote service password."
    }
}

foreach ($item in $selected) {
    $password = Read-SecretText "$($item.Name) password"
    [IO.File]::WriteAllText($item.File, $password, $utf8NoBom)
}

if ($IsWindows -or $env:OS -eq 'Windows_NT') {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    foreach ($item in $selected) {
        & icacls.exe $item.File /inheritance:r /grant:r "${identity}:(F)" '*S-1-5-18:(F)' '*S-1-5-32-544:(F)' | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to restrict permissions on $($item.File)"
        }
    }
}

Write-Host "$Target Docker secret configuration written to $privateDirectory"
