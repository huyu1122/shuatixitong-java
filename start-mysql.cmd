@echo off
chcp 65001 >nul 2>&1
title MySQL 8.0 - 本地开发数据库
set "MYSQL_HOME=%LOCALAPPDATA%\Programs\mysql\mysql-8.0.28-winx64"
set "MY_INI=%LOCALAPPDATA%\Programs\mysql\my.ini"

echo.
echo  ==================================================
echo    MySQL 8.0.28 - 学而刷题系统数据库
echo  ==================================================
echo.
echo   端口    : 3306
echo   用户名  : root
echo   密码    : root
echo   数据库  : quiz_system
echo.
echo   关闭此窗口即停止 MySQL 服务。
echo.

if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
    echo   [错误] 未找到 MySQL，请检查安装路径：
    echo          %MYSQL_HOME%
    echo.
    pause
    exit /b 1
)

if not exist "%MY_INI%" (
    echo   [错误] 未找到配置文件：
    echo          %MY_INI%
    echo.
    pause
    exit /b 1
)

echo   正在启动...
echo.

"%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%MY_INI%" --console

echo.
echo   MySQL 已停止。
pause