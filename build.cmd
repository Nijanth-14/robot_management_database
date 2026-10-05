@echo off
setlocal
cd /d "%~dp0"
call scripts\java-env.cmd
if errorlevel 1 exit /b 1
if exist ".tools\apache-maven-3.9.11\bin\mvn.cmd" (
    call ".tools\apache-maven-3.9.11\bin\mvn.cmd" "-Dmaven.repo.local=%CD%\.m2" --batch-mode package %*
    if errorlevel 1 exit /b 1
    exit /b 0
)
where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven is missing. Install Apache Maven or place Maven 3.9.11 in .tools.
    exit /b 1
)
call mvn --batch-mode package %*
if errorlevel 1 exit /b 1
exit /b 0
