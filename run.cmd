@echo off
setlocal
cd /d "%~dp0"
call scripts\java-env.cmd
if errorlevel 1 (
  pause
  exit /b 1
)
set "CALENDAR_JAR=%~dp0calendar.jar"
if exist "%CALENDAR_JAR%" goto launch
set "CALENDAR_JAR=%~dp0target\calendar.jar"
if exist "%CALENDAR_JAR%" goto launch
if not exist "%~dp0pom.xml" (
  echo calendar.jar is missing. Extract the complete Calendar ZIP archive first.
  pause
  exit /b 1
)
call build.cmd
if errorlevel 1 (
  pause
  exit /b 1
)
:launch
if not defined PORT set "PORT=8080"
echo Open http://localhost:%PORT% in your browser. Keep this window open.
"%CALENDAR_JAVA%" -jar "%CALENDAR_JAR%" %*
set "CALENDAR_EXIT=%ERRORLEVEL%"
if not "%CALENDAR_EXIT%"=="0" (
  echo Calendar could not start. It requires Java 25 or newer. Check the error above.
  pause
)
exit /b %CALENDAR_EXIT%
