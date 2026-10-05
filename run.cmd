@echo off
setlocal
cd /d "%~dp0"
call scripts\java-env.cmd
if errorlevel 1 exit /b 1
if not exist "target\robot-monitoring.jar" (
    echo Build the application first using build.cmd.
    exit /b 1
)
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -jar "target\robot-monitoring.jar" %*
    if errorlevel 1 exit /b 1
    exit /b 0
)
java -jar "target\robot-monitoring.jar" %*
if errorlevel 1 exit /b 1
exit /b 0
