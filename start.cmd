@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME if exist "%USERPROFILE%\.jdks\ms-25.0.4.1\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\ms-25.0.4.1"
if not defined JAVA_HOME (
  echo Set JAVA_HOME to a JDK 25 installation and try again.
  pause
  exit /b 1
)
set "MAVEN_USER_HOME=%~dp0.mvn\user-home"
echo Calendar will be available at http://localhost:8080
call mvnw.cmd spring-boot:run
if errorlevel 1 pause
