@echo off
setlocal

:: 要启动的服务列表
set SERVICES=db-manager CommentService MessageService RecommendationService UserService VideoService DanmakuService ReportService HistoryService

@REM echo 正在启动nginx
@REM wt -w 0 new-tab -d ..\..\nginx --title nginx cmd /k .\nginx

echo 正在启动docker
wt -w 0 new-tab -d ..\media\ --title docker-page cmd /k docker-compose up -d

echo 正在启动minio-logger
wt -w 0 new-tab -d ..\media\ --title minio-logger cmd /k python webhook.py

:: 遍历每个服务并启动
for %%S in (%SERVICES%) do (
    echo 正在启动 %%S ...
    @REM start cmd /c "cd /d ".\%%S" && sbt run"
    wt -w 0 new-tab -d .\%%S --title %%S cmd /k sbt run
    timeout /t 1 /nobreak >nul
)

echo 所有服务已启动！
endlocal