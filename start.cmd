@echo off
setlocal
cd /d "%~dp0"
call scripts\java-env.cmd
if errorlevel 1 (
  pause
  exit /b 1
)
set "MAVEN_USER_HOME=%~dp0.mvn\user-home"
echo Calendar will be available at http://localhost:8080
call mvnw.cmd spring-boot:run
set "CALENDAR_EXIT=%ERRORLEVEL%"
if not "%CALENDAR_EXIT%"=="0" pause
exit /b %CALENDAR_EXIT%
