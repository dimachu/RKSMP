[CmdletBinding()]
param(
    [ValidateSet('Configure', 'Run')]
    [string]$Action = 'Run'
)

$ErrorActionPreference = 'Stop'
$privateDir = Join-Path $env:LOCALAPPDATA 'Module6-Neon'
$privateFile = Join-Path $privateDir 'connection.dpapi'
$secureInput = $null

function Read-NeonConnection([securestring]$Connection) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Connection)
    try {
        $rawConnection = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer).Trim()
        if ($rawConnection -match '^psql\s+[''"](?<connection>postgres(?:ql)?://[^''"]+)[''"]\s*$') {
            $rawConnection = $Matches.connection
        }
        $parsed = $null
        if (-not [Uri]::TryCreate($rawConnection, [UriKind]::Absolute, [ref]$parsed)) {
            throw 'Нужна строка PostgreSQL из окна Connect.'
        }
        $userInfo = $parsed.UserInfo -split ':', 2
        if ($parsed.Scheme -notin 'postgres', 'postgresql' -or
            $parsed.Host -notmatch '^ep-[a-z0-9-]+\.[a-z0-9.-]+\.neon\.tech$' -or
            $parsed.AbsolutePath -notmatch '^/[a-zA-Z0-9_%-]+$' -or
            $userInfo.Count -ne 2 -or -not $userInfo[0] -or -not $userInfo[1] -or $userInfo[1] -match '^\*+$') {
            throw 'Выберите учебную базу в Neon Connect и скопируйте полную строку PostgreSQL.'
        }
        $tls = 'sslmode=require'
        if ($parsed.Query -match '(?:\?|&)channel_binding=require(?:&|$)') {
            $tls += '&channelBinding=require'
        }
        [pscustomobject]@{
            Url = "jdbc:postgresql://$($parsed.Host):5432$($parsed.AbsolutePath)?$tls"
            User = [Uri]::UnescapeDataString($userInfo[0])
            Password = [Uri]::UnescapeDataString($userInfo[1])
        }
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

if ($Action -eq 'Configure') {
    $secureInput = Read-Host 'Вставьте строку из Neon Connect и нажмите Enter' -AsSecureString
    $null = Read-NeonConnection $secureInput
    [void](New-Item -Path $privateDir -ItemType Directory -Force)
    $secureInput | ConvertFrom-SecureString | Set-Content -LiteralPath $privateFile -Encoding ascii
    Write-Output 'Параметры сохранены для текущей учётной записи Windows.'
    return
}

if (-not (Test-Path -LiteralPath $privateFile)) {
    throw 'Сначала запустите этот файл с -Action Configure.'
}
$secureInput = (Get-Content -Raw -LiteralPath $privateFile).Trim() | ConvertTo-SecureString
$connection = Read-NeonConnection $secureInput
$jwtBytes = New-Object byte[] 48
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($jwtBytes)
} finally {
    $random.Dispose()
}
$protectedNames = 'JDBC_URL', 'DB_USER', 'DB_PASSWORD', 'JWT_SECRET'
$previous = @{}
foreach ($name in $protectedNames) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$exitCode = 1
Push-Location -LiteralPath $PSScriptRoot
try {
    $env:JDBC_URL = $connection.Url
    $env:DB_USER = $connection.User
    $env:DB_PASSWORD = $connection.Password
    $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
    & .\gradlew.bat --no-daemon --console=plain :serverNeon:run
    $exitCode = $LASTEXITCODE
} finally {
    foreach ($name in $protectedNames) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
    [Array]::Clear($jwtBytes, 0, $jwtBytes.Length)
    $secureInput.Dispose()
    Pop-Location
}
exit $exitCode
