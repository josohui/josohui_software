@echo off
cd /d "%~dp0"
java -jar dist\ssd_ip_cp4_2026f_252482_app.jar
if errorlevel 1 pause
