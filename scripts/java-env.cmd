@rem Find Java without changing system settings. This file is called by other scripts.
@set "CALENDAR_JAVA="
@if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" (
    set "CALENDAR_JAVA=%JAVA_HOME%\bin\java.exe"
    exit /b 0
  )
  echo JAVA_HOME does not point to a Java installation.
  exit /b 1
)
@where java >nul 2>nul
@if not errorlevel 1 (
  set "CALENDAR_JAVA=java"
  exit /b 0
)
@for %%V in (26 25) do @for /d %%D in ("%USERPROFILE%\.jdks\*%%V*") do @if exist "%%~fD\bin\java.exe" (
  set "JAVA_HOME=%%~fD"
  set "CALENDAR_JAVA=%%~fD\bin\java.exe"
)
@if defined CALENDAR_JAVA exit /b 0
@echo Java 25 or newer is required. Install Java and set JAVA_HOME or add java to PATH.
@exit /b 1
