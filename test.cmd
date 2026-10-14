@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME if exist "%USERPROFILE%\.jdks\ms-25.0.4.1\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\ms-25.0.4.1"
set "MAVEN_USER_HOME=%~dp0.mvn\user-home"
call mvnw.cmd test
exit /b %ERRORLEVEL%
