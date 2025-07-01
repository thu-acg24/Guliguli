@echo off
chcp 65001
setlocal enabledelayedexpansion

REM 设置密码文件路径（建议使用绝对路径）
set PASSWORD_FILE=%~dp0minio_pwd.txt
set DATA_FILE=%~dp0data
set CONFIG_FILE=%~dp0config
echo %PASSWORD_FILE%
echo %DATA_FILE%
echo %CONFIG_FILE%

REM 检查密码文件是否存在
if not exist "%PASSWORD_FILE%" (
    echo 错误：密码文件未找到: %PASSWORD_FILE%
    echo 请创建包含密码的文本文件
    exit /b 1
)

REM 运行 MinIO 容器
docker run -d ^
  -p 9000:9000 ^
  -p 9001:9001 ^
  -v %DATA_FILE%:/data ^
  -v %CONFIG_FILE%:/root/.minio ^
  -e "MINIO_OPTS=--address 0.0.0.0:9001" ^
  -e MINIO_ROOT_USER=gugugaga ^
  --mount type=bind,source="%PASSWORD_FILE%",target=/run/secrets/minio_pwd,readonly ^
  -e MINIO_ROOT_PASSWORD_FILE=/run/secrets/minio_pwd ^
  --name minio-with-ffmpeg ^
  custom-minio ^
  server /data --console-address ":9001"

echo MinIO 已启动：
echo 管理界面: http://localhost:9001
echo API 端点: http://localhost:9000