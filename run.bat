@echo off
setlocal

rem Quick launcher. Double-click to start from level 1.
rem Command line usage:
rem   run.bat            start from level 1
rem   run.bat 3          start from level 3 (1 to 6)
rem   run.bat test       run the self-check test
rem   run.bat editor     open the level editor
rem   run.bat editor 3   open the level editor on level 3

set "SCRIPT=%~dp0build.ps1"

if /i "%~1"=="test" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Test
) else if /i "%~1"=="editor" (
    if "%~2"=="" (
        powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Editor
    ) else (
        powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Editor -Level %~2
    )
) else if "%~1"=="" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Level %~1
)

if errorlevel 1 (
    echo.
    echo Failed to run. Please check the error messages above.
    pause
)

endlocal
