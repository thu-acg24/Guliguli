# Guliguli 文件系统架构 - Media功能说明

## 概述

Guliguli 项目的文件系统采用 minIO 进行存储，文件处理服务使用 Flask，本服务提供图片和视频处理功能，包括图片格式转换（转JPG）和视频转码切片（HLS格式）。服务与MinIO对象存储集成，使用GPU加速视频处理。

## 功能特性

### 图片处理

- 支持格式：JPG, PNG, WEBP → 统一转换为JPG
- 尺寸限制：最大1024×1024像素
- 存储位置：`avatar` 或 `video-cover` 桶
- 路径格式：`{user_id}/{uuid}.jpg`

### 视频处理

- 支持格式：常见视频格式（MP4, MOV, AVI等）
- 验证条件：
  - 分辨率：100x100 到 3840x2160
  - 时长：1秒 到 60分钟
- 输出格式：HLS（.m3u8 + .ts切片）
- GPU加速：使用NVIDIA CUDA硬件加速
- 存储位置：`video-server` 桶
- 路径格式：`{video_id}/{video_name}/index.m3u8`，其中 `video_name` 为我们的 `VideoService` 后端生成的名字，格式为 `[timestamp]_[uuid]`，因此唯一性可以保证

## API端点

### 图片处理

- **URL**: `/image`

- **方法**: `POST`

- **参数**:

  - `id`: 用户ID
  - `file_name`: MinIO 临时桶中的文件名
  - `task`: 任务类型 (`avatar` 或 `cover`)

- **响应**:

  json

  ```
  {
    "status": "success" | "failure",
    "message": "存储路径或错误信息"
  }
  ```

### 视频处理

- **URL**: `/video`

- **方法**: `POST`

- **参数**:

  - `id`: 用户ID
  - `token`: 回调令牌
  - `file_name`: MinIO临时桶中的文件名

- **响应**:

  json

  ```
  {
    "status": "success" | "failure",
    "message": "处理状态信息"
  }
  ```

## 回调接口

视频处理完成后会调用配置的`CALLBACK_API`，回调数据结构：

```
{
  "token": "原始令牌",
  "status": "success" | "failure",
  "m3u8_path": "HLS主文件路径",
  "ts_path_prefix": "TS文件前缀",
  "ts_count": 切片数量,
  "serviceName": "Tong-Wen",
  "message_type": "配置的回调名称"
}
```

## 部署指南

### 环境要求

- Docker 20.10+
- Docker Compose 1.29+
- NVIDIA GPU驱动
- NVIDIA Container Toolkit

### 配置文件 (.env)

```
# MinIO配置
MINIO_ROOT_USER=your_access_key
MINIO_ROOT_PASSWORD=your_secret_key

# 服务配置
MINIO_ENDPOINT=minio:5003 # 需要和docker-compose中的设置匹配
CALLBACK_API=https://your-api/callback # VideoService的api链接
CALLBACK_API_NAME=video_processed # VideoService中route里声明的api名字
```

### 启动服务

```
docker-compose up -d --build
```

### 服务状态检查

```
docker-compose logs -f media-processor
```