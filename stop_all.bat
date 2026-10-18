@echo off
rem One-click stop for 老年综合评估系统
title Stop All Services
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0stop_all.ps1"
echo.
pause
