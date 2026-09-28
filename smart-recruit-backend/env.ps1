# =============================================================================
#  Load .env into the current PowerShell session.
#
#  Usage - note the leading dot and space (dot-sourcing is required, otherwise
#  the variables are set in a child process and discarded):
#      . .\env.ps1
#      mvn -pl smart-recruit-ai-engine spring-boot:run
#
#  Self-check without printing key contents:
#      . .\env.ps1 --check
#
#  Keep this file ASCII-only: Windows PowerShell 5.1 reads .ps1 as ANSI/GBK
#  when the file has no BOM, so non-ASCII here would be mis-decoded.
#  This script only reads .env - it contains no secrets itself.
# =============================================================================

$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path $envFile)) {
    Write-Host "[env.ps1] .env not found: $envFile"
    return
}

$loaded = 0
foreach ($line in Get-Content $envFile) {
    $t = $line.Trim()
    if ($t -eq '' -or $t.StartsWith('#')) { continue }
    $i = $t.IndexOf('=')
    if ($i -lt 1) { continue }
    $k = $t.Substring(0, $i).Trim()
    $v = $t.Substring($i + 1).Trim()
    Set-Item -Path "Env:$k" -Value $v
    $loaded++
}

if ($args -contains '--check') {
    Write-Host "[env.ps1] loaded $loaded variables from .env"
    if ($env:COMPUTER_ID)      { Write-Host "   COMPUTER_ID      = $env:COMPUTER_ID" }
    if ($env:QWEN_API_KEY)     { Write-Host "   QWEN_API_KEY     = $($env:QWEN_API_KEY.Substring(0,7))..." }
    if ($env:DEEPSEEK_API_KEY) { Write-Host "   DEEPSEEK_API_KEY = $($env:DEEPSEEK_API_KEY.Substring(0,7))..." }
    if (-not "$env:COMPUTER_ID$env:QWEN_API_KEY$env:DEEPSEEK_API_KEY") {
        Write-Host "   [!] all three key vars are empty - check that .env exists and keys are filled"
    } else {
        Write-Host "   [OK] ready to start services"
    }
} else {
    Write-Host "[env.ps1] loaded $loaded environment variables into this PowerShell session"
    if (-not $env:COMPUTER_ID)  { Write-Host "[env.ps1] [!] COMPUTER_ID is empty - Nacos discovery group may be wrong" }
    if (-not $env:QWEN_API_KEY) { Write-Host "[env.ps1] [!] QWEN_API_KEY is empty - AI will fall back to rules only" }
}
