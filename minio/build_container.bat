@echo off
chcp 65001
setlocal enabledelayedexpansion

REM 设置文件路径（建议使用绝对路径）
set DATA_FILE=%~dp0data
set CONFIG_FILE=%~dp0config
echo %DATA_FILE%
echo %CONFIG_FILE%

REM 检查文件夹是否存在
if not exist "%DATA_FILE%" (
    echo 错误：数据存放文件夹未找到: %DATA_FILE%
    echo 请创建用来存储数据的文件夹
    exit /b 1
)

REM 运行 MinIO 容器
docker run -d ^
  -p 9000:9000 ^
  -p 9001:9001 ^
  -v %DATA_FILE%:/data ^
  -v %CONFIG_FILE%:/root/.minio ^
  --env-file .env ^
  --name minio-service ^
  minio/minio ^
  server /data --console-address ":9001"

echo MinIO 已启动：
echo 管理界面: http://localhost:9001
echo API 端点: http://localhost:9000