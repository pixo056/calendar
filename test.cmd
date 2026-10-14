@echo off
setlocal
cd /d "%~dp0"
call scripts\java-env.cmd
if errorlevel 1 exit /b 1
set "MAVEN_USER_HOME=%~dp0.mvn\user-home"
call mvnw.cmd test
exit /b %ERRORLEVEL%
