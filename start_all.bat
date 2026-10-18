@echo off
rem One-click start for 老年综合评估系统
title Start All Services
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0start_all.ps1"
echo.
pause
