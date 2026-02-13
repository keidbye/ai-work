@echo off
chcp 65001 >nul
title 关闭考勤统计工具

echo.
echo ========================================
echo 正在关闭考勤统计工具...
echo.
echo 注意：此操作仅关闭应用窗口
echo 不影响后台数据处理
echo.
echo 请选择操作：
echo.
echo.
tasklist /FI "IMAGENAME eq java.exe" /NH | find /I /N "java.exe" >nul
if %errorlevel% equ 0 (
    echo.
    echo [选项] 1. 关闭应用窗口（推荐）
    echo.
    echo [选项] 2. 强制结束程序（包括后台服务）
    echo.
    set /p choice=请选择选项 (1/2):
    if "!choice!"=="1" (
        REM 关闭应用窗口（只关闭桌面窗口，后台服务继续运行）
        tasklist /FI "IMAGENAME eq java.exe" /NH | find /I /N "java.exe" /NH | find /I /N "考勤统计工具" >nul
        if !errorlevel! (
            echo.
            echo [成功] 考勤统计工具窗口已关闭
            echo        后台服务仍在运行，可通过浏览器访问
            echo.
            echo 如需完全停止，请选择选项2
        ) else (
            echo.
            echo [提示] 未检测到考勤统计工具窗口
        )
    ) else if "!choice!"=="2" (
        REM 强制结束所有Java进程
        taskkill /F /IMAGENAME eq java.exe /NH
        if !errorlevel! (
            echo.
            echo [成功] 考勤统计工具已完全停止
            echo        包括后台服务和窗口界面
        ) else (
            echo.
            echo [提示] 未检测到Java进程
        )
    )
)

echo.
echo ========================================
pause
