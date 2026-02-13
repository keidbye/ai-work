@echo off
chcp 65001 >nul
title 考勤统计工具

echo.
echo ========================================
echo   考勤统计工具 - 启动中...
echo.

REM 检查Java版本
for /f "tokens=2 delims=." %%a in ('java -fullversion 2^>1') do (
    set JAVA_VERSION=%%a
)

echo 检测到 JDK %JAVA_VERSION%

REM 检查JavaFX模块
javafx -version >nul 2>&1
if %errorlevel% equ 0 (
    echo [成功] JavaFX模块已就绪
) else (
    echo [错误] 未检测到JavaFX模块，请确保已安装JavaFX 17
    pause
    exit /b 1
)

REM 启动桌面应用
echo [INFO] 正在启动桌面应用...
start "" "考勤统计工具桌面" com.example.aiwork.desktop.DesktopAppLauncher

timeout /t 10 >nul

REM 检查应用是否还在运行
tasklist /FI "IMAGENAME eq java.exe" 2>nul
if %errorlevel% equ 0 (
    echo.
    echo [INFO] 桌面应用已启动，请使用应用窗口操作
    echo.
    echo 如未看到窗口，请检查：
    echo   1. 已安装JavaFX 17
    echo   2. 防火墙/杀毒软件允许运行
    echo   3. 端口8080未被占用
    echo.
    timeout /t 30 >nul
) else (
    echo.
    echo [成功] 桌面应用正在运行
)

echo.
echo ========================================
pause
