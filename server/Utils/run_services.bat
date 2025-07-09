@echo off
setlocal

:: 要启动的服务列表
set SERVICES=CommentService MessageService RecommendationService UserService VideoService DanmakuService ReportService HistoryService

echo 正在启动db-manager
@REM start cmd /c "cd /d ..\..\db-manager && .\bin\db-manager.bat"
wt -w 0 new-tab -d ..\..\db-manager --title db-manager cmd /k .\bin\db-manager.bat

:: 遍历每个服务并启动
for %%S in (%SERVICES%) do (
    echo 正在启动 %%S ...
    @REM start cmd /c "cd /d ".\%%S" && sbt run"
    wt -w 0 new-tab -d .\%%S --title %%S cmd /k sbt run
    timeout /t 1 /nobreak >nul
)

echo 所有服务已启动！
endlocal