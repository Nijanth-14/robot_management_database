@echo off
rem Called by build.cmd/run.cmd; environment changes remain inside their setlocal.
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" exit /b 0
where javac >nul 2>nul
if not errorlevel 1 exit /b 0
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\javac.exe" (
    set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    exit /b 0
)
echo Java JDK 17 or newer is required. Set JAVA_HOME to its installation folder.
exit /b 1
