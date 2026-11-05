@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
 echo Java 17 or newer is required. Please install Java and run this file again.
 pause
 exit /b 1
)
if not exist "dist\ssd_ip_cp4_2026f_252482_app.jar" (
 echo The game file is missing. Extract the entire downloaded ZIP before running.
 pause
 exit /b 1
)
java -jar "dist\ssd_ip_cp4_2026f_252482_app.jar"
if errorlevel 1 (
 echo Please check that Java 17 or newer is installed.
 pause
)
