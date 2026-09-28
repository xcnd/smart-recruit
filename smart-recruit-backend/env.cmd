@echo off
REM ============================================================================
REM  Load .env into the current CMD session.
REM
REM  Usage (the leading "call" is required - do NOT double-click this file):
REM      call env.cmd
REM      mvn -pl smart-recruit-ai-engine spring-boot:run
REM
REM  Run "call env.cmd" once in every CMD window before starting a service.
REM  Self-check without printing key contents:
REM      call env.cmd --check
REM
REM  NOTE: keep this file ASCII-only and CRLF. Windows CMD reads .cmd in the
REM  OEM codepage (GBK on zh-CN), so UTF-8 Chinese here would break parsing.
REM  This script only reads .env - it contains no secrets itself.
REM ============================================================================

set "_n=0"
for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /v "#" "%~dp0.env"`) do (
    if not "%%A"=="" (
        call set "%%A=%%B"
        set /a _n+=1
    )
)

if /i "%~1"=="--check" (
    echo [env.cmd] loaded %_n% variables from .env
    if not "%COMPUTER_ID%"==""       echo    COMPUTER_ID      = %COMPUTER_ID%
    if not "%QWEN_API_KEY%"==""      echo    QWEN_API_KEY     = %QWEN_API_KEY:~0,7%...
    if not "%DEEPSEEK_API_KEY%"==""  echo    DEEPSEEK_API_KEY = %DEEPSEEK_API_KEY:~0,7%...
    if "%COMPUTER_ID%%QWEN_API_KEY%%DEEPSEEK_API_KEY%"=="" (
        echo    [!] all three key vars are empty - check that .env exists and keys are filled
    ) else (
        echo    [OK] ready to start services
    )
) else (
    echo [env.cmd] loaded %_n% environment variables into this CMD session
    if "%COMPUTER_ID%"=="" echo [env.cmd] [!] COMPUTER_ID is empty - Nacos discovery group may be wrong
    if "%QWEN_API_KEY%"=="" echo [env.cmd] [!] QWEN_API_KEY is empty - AI will fall back to rules only
)

set "_n="
