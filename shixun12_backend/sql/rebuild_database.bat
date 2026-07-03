@echo off
chcp 65001 >nul
echo ========================================
echo TeamMatch 数据库重建工具
echo ========================================
echo.
echo 警告：此操作将删除现有数据库！
echo.
set /p confirm="确认继续？(y/n): "
if /i not "%confirm%"=="y" (
    echo 已取消操作
    pause
    exit /b
)

echo.
echo 正在执行数据库重建...
echo.

mysql -u root -p < "%~dp0rebuild_database.sql"

if %errorlevel% equ 0 (
    echo.
    echo ✅ 数据库重建成功！
) else (
    echo.
    echo ❌ 数据库重建失败，请检查错误信息
)

echo.
pause
